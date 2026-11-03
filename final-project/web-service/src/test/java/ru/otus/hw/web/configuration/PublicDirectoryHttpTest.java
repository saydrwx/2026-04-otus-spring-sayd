package ru.otus.hw.web.configuration;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.web.client.RestClient;
import ru.otus.hw.web.client.RestDirectoryClient;
import ru.otus.hw.web.exception.DirectoryUnavailableException;
import ru.otus.hw.web.resilience.DirectoryReadExecutor;

import static org.assertj.core.api.Assertions.*;

class PublicDirectoryHttpTest {

  WebProperties properties(URI origin) {
    return new WebProperties(origin, origin, origin, origin, origin, "test-client-secret-1234",
      Duration.ofSeconds(1), Duration.ofMillis(150), Duration.ofSeconds(1));
  }

  DirectoryReadExecutor reads() {
    var p = new WebDirectoryResilienceProperties(2, Duration.ofMillis(1), 6, 3, 50,
      Duration.ofSeconds(10), 2);
    var config = new WebDirectoryResilienceConfiguration();
    return new DirectoryReadExecutor(config.webDirectoryRetry(p),
      config.webDirectoryCircuitBreaker(p));
  }

  @Test
  @Timeout(5)
  void realTransportDoesNotFollowDirectoryRedirects() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var followed = new AtomicInteger();
    server.createContext("/api/hospitals", exchange -> {
      exchange.getResponseHeaders().set("Location", "/unexpected");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });
    server.createContext("/unexpected", exchange -> {
      followed.incrementAndGet();
      exchange.sendResponseHeaders(200, 0);
      exchange.close();
    });
    server.start();
    try {
      var origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
      var http = new DirectoryConfiguration().directoryRestClient(RestClient.builder(),
        properties(origin));
      assertThatThrownBy(() -> new RestDirectoryClient(http, reads()).hospitals()).isInstanceOf(
        DirectoryUnavailableException.class);
      assertThat(followed.get()).isZero();
    } finally {
      server.stop(0);
    }
  }

  @Test
  @Timeout(5)
  void stalledResponseHeadersAreBounded() throws Exception {
    assertTimeouts(false);
  }

  @Test
  @Timeout(5)
  void stalledResponseBodyIsBounded() throws Exception {
    assertTimeouts(true);
  }

  private void assertTimeouts(boolean sendHeaders) throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var calls = new AtomicInteger();
    var workers = Executors.newCachedThreadPool(r -> {
      var thread = new Thread(r);
      thread.setDaemon(true);
      return thread;
    });
    server.setExecutor(workers);
    server.createContext("/api/hospitals", exchange -> {
      calls.incrementAndGet();
      if (sendHeaders) {
        exchange.sendResponseHeaders(200, 2);
      }
      try {
        Thread.sleep(2000);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      } finally {
        exchange.close();
      }
    });
    server.start();
    try {
      var origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
      var http = new DirectoryConfiguration().directoryRestClient(RestClient.builder(),
        properties(origin));
      long start = System.nanoTime();
      assertThatThrownBy(() -> new RestDirectoryClient(http, reads()).hospitals()).isInstanceOf(
        DirectoryUnavailableException.class);
      assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(4));
      assertThat(calls.get()).isEqualTo(2);
    } finally {
      server.stop(0);
      workers.shutdownNow();
    }
  }
}
