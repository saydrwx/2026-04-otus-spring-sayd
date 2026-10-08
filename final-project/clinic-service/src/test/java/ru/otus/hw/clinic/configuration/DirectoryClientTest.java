package ru.otus.hw.clinic.configuration;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.otus.hw.clinic.client.RestClientDirectoryClient;
import ru.otus.hw.clinic.exception.DirectoryUnavailableException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.exception.TransientDirectoryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DirectoryClientTest {

  private static final String URL = "http://directory.test/api/doctors/1";
  private static final String PROVIDER = """
    {"id":1,"firstName":"Ivan","hospital":{"id":2,"name":"Hospital"},"specialties":[]}
    """;
  private MockRestServiceServer server;
  private RestClientDirectoryClient client;
  private CircuitBreaker breaker;
  private final DirectoryClientConfiguration configuration = new DirectoryClientConfiguration();

  @BeforeEach
  void setUp() {
    var builder = RestClient.builder().baseUrl("http://directory.test");
    server = MockRestServiceServer.bindTo(builder).build();
    breaker = configuration.directoryCircuitBreaker();
    client = new RestClientDirectoryClient(builder.build(), configuration.directoryRetry(),
      breaker);
  }

  @AfterEach
  void verifyRequests() {
    server.verify();
  }

  @Test
  void consumesOnlyProviderIdentityAndHospital() {
    server.expect(requestTo(URL)).andRespond(withSuccess(PROVIDER, MediaType.APPLICATION_JSON));
    var doctor = client.findDoctor(1);
    assertThat(doctor.id()).isEqualTo(1);
    assertThat(doctor.hospital().id()).isEqualTo(2);
  }

  @Test
  void retriesTransientFailureThenSucceeds() {
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server.expect(requestTo(URL)).andRespond(withSuccess(PROVIDER, MediaType.APPLICATION_JSON));
    assertThat(client.findDoctor(1).hospital().id()).isEqualTo(2);
    assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
  }

  @ParameterizedTest
  @ValueSource(ints = {500, 502, 503, 504})
  void transientHttpFailuresAreLimitedToTwoAttempts(int status) {
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)));
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)));
    assertThatThrownBy(() -> client.findDoctor(1)).isInstanceOf(TransientDirectoryException.class);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
  }

  @Test
  void transportTimeoutIsRetriedOnlyOnce() {
    server.expect(requestTo(URL)).andRespond(request -> {
      throw new SocketTimeoutException("test");
    });
    server.expect(requestTo(URL)).andRespond(request -> {
      throw new SocketTimeoutException("test");
    });
    assertThatThrownBy(() -> client.findDoctor(1)).isInstanceOf(TransientDirectoryException.class);
  }

  @Test
  void missingDoctorIsNeitherRetriedNorRecordedAsDependencyFailure() {
    for (int index = 0; index < 4; index++) {
      server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));
    }
    for (int index = 0; index < 4; index++) {
      assertThatThrownBy(() -> client.findDoctor(1)).isInstanceOf(ResourceNotFoundException.class);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 429, 501})
  void unexpectedStatusesAreNotRetriedOrExposed(int status) {
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status))
      .body("private upstream error"));
    assertThatThrownBy(() -> client.findDoctor(1)).isInstanceOf(DirectoryUnavailableException.class)
      .isNotInstanceOf(TransientDirectoryException.class)
      .hasMessageNotContaining("private upstream error");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not-json", "{}", "{\"id\":1}", "{\"id\":2,\"hospital\":{\"id\":2}}",
    "{\"id\":1,\"hospital\":{\"id\":0}}"})
  void invalidBodiesAreNotRetried(String body) {
    server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    assertThatThrownBy(() -> client.findDoctor(1)).isInstanceOf(DirectoryUnavailableException.class)
      .isNotInstanceOf(TransientDirectoryException.class);
  }

  @Test
  void opensCircuitAfterThreeFailedLogicalCallsAndFailsFastAfterward() {
    for (int index = 0; index < 6; index++) {
      server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    }
    for (int index = 0; index < 3; index++) {
      assertThatThrownBy(() -> client.findDoctor(1)).isInstanceOf(
        DirectoryUnavailableException.class);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    // No seventh HTTP expectation: a short-circuited call must not reach the server.
    assertThatThrownBy(() -> client.findDoctor(1)).isInstanceOf(
      DirectoryUnavailableException.class);
    assertThat(breaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
  }

  @Test
  void twoSuccessfulHalfOpenProbesCloseCircuit() {
    breaker.transitionToOpenState();
    breaker.transitionToHalfOpenState();
    server.expect(requestTo(URL)).andRespond(withSuccess(PROVIDER, MediaType.APPLICATION_JSON));
    server.expect(requestTo(URL)).andRespond(withSuccess(PROVIDER, MediaType.APPLICATION_JSON));
    client.findDoctor(1);
    client.findDoctor(1);
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void configuredReadTimeoutWorksAgainstARealLocalHttpServer() throws Exception {
    var release = new CountDownLatch(1);
    var count = new AtomicInteger();
    var executor = Executors.newCachedThreadPool();
    var localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    localServer.setExecutor(executor);
    localServer.createContext("/api/doctors/1", exchange -> {
      count.incrementAndGet();
      try {
        release.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      } finally {
        exchange.close();
      }
    });
    localServer.start();
    try {
      var properties = new DirectoryProperties(
        URI.create("http://127.0.0.1:" + localServer.getAddress().getPort()),
        Duration.ofMillis(200), Duration.ofMillis(100));
      var realClient = new RestClientDirectoryClient(
        configuration.directoryRestClient(RestClient.builder(), properties),
        configuration.directoryRetry(), configuration.directoryCircuitBreaker());
      assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
        assertThatThrownBy(() -> realClient.findDoctor(1)).isInstanceOf(
          TransientDirectoryException.class));
      assertThat(count.get()).isEqualTo(2);
    } finally {
      release.countDown();
      localServer.stop(0);
      executor.shutdownNow();
    }
  }
}
