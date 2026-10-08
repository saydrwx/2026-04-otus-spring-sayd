package ru.otus.hw.web.configuration;

import java.time.Duration;
import java.util.List;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.otus.hw.web.client.RestDirectoryClient;
import ru.otus.hw.web.configuration.*;
import ru.otus.hw.web.dto.DoctorSearchInput;
import ru.otus.hw.web.exception.*;
import ru.otus.hw.web.resilience.DirectoryReadExecutor;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DirectoryResilienceTest {

  MockRestServiceServer server;
  RestDirectoryClient client;
  CircuitBreaker breaker;
  Retry retry;

  @BeforeEach
  void setup() {
    var builder = org.springframework.web.client.RestClient.builder()
      .baseUrl("http://directory.test");
    server = MockRestServiceServer.bindTo(builder).build();
    var properties = new WebDirectoryResilienceProperties(2, Duration.ofMillis(1), 6, 3, 50,
      Duration.ofSeconds(10), 2);
    var config = new WebDirectoryResilienceConfiguration();
    retry = config.webDirectoryRetry(properties);
    breaker = config.webDirectoryCircuitBreaker(properties);
    client = new RestDirectoryClient(builder.build(), new DirectoryReadExecutor(retry, breaker));
  }

  @Test
  void transientReadRetriesOnceAndCountsOneLogicalSuccess() {
    server.expect(requestTo("http://directory.test/api/doctors"))
      .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server.expect(requestTo("http://directory.test/api/doctors"))
      .andExpect(headerDoesNotExist("Authorization"))
      .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    assertThat(client.doctors(new DoctorSearchInput(null, List.of()))).isEmpty();
    server.verify();
    assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
  }

  @Test
  void transportFailuresAreRetriedButNeverBecomeEmptySuccess() {
    server.expect(ExpectedCount.times(2), anything()).andRespond(request -> {
      throw new java.io.IOException("private upstream details");
    });
    assertThatThrownBy(() -> client.hospitals()).isInstanceOf(DirectoryUnavailableException.class)
      .hasMessageNotContaining("private");
    server.verify();
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
  }

  @Test
  void ordinary4xxAndUnselected5xxAreNotRetriedOrRecordedAsDependencyFailures() {
    for (HttpStatus status : List.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED,
      HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND, HttpStatus.CONFLICT, HttpStatus.TOO_MANY_REQUESTS,
      HttpStatus.NOT_IMPLEMENTED)) {
      setup();
      server.expect(anything()).andRespond(withStatus(status));
      if (status == HttpStatus.NOT_FOUND) {
        assertThatThrownBy(() -> client.doctor(99)).isInstanceOf(DoctorNotFoundException.class);
      } else {
        assertThatThrownBy(() -> client.doctor(99)).isInstanceOf(
          DirectoryUnavailableException.class);
      }
      server.verify();
      assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
      assertThat(breaker.getMetrics().getNumberOfSuccessfulCalls()).isZero();
    }
  }

  @Test
  void malformedEmptyAndStructurallyInvalidPayloadsAreNotRetried() {
    for (String body : List.of("not-json", "", "[{\"id\":1}]")) {
      setup();
      server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
      assertThatThrownBy(() -> client.doctors(new DoctorSearchInput(null, List.of()))).isInstanceOf(
        DirectoryUnavailableException.class);
      server.verify();
    }
  }

  @Test
  void breakerOpensAfterThreeFailedLogicalReadsAndSkipsFurtherHttp() {
    server.expect(ExpectedCount.times(6), anything())
      .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> client.hospitals()).isInstanceOf(
        DirectoryUnavailableException.class);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThatThrownBy(() -> client.specialties()).isInstanceOf(
      DirectoryUnavailableException.class);
    server.verify();
    assertThat(breaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
  }

  @Test
  void twoSuccessfulHalfOpenProbesCloseAndFailedProbeReopens() {
    breaker.transitionToOpenState();
    breaker.transitionToHalfOpenState();
    server.expect(ExpectedCount.times(2), anything())
      .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    client.hospitals();
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    client.specialties();
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    server.verify();
    setup();
    breaker.transitionToOpenState();
    breaker.transitionToHalfOpenState();
    server.expect(ExpectedCount.times(4), anything())
      .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
    assertThatThrownBy(() -> client.hospitals()).isInstanceOf(DirectoryUnavailableException.class);
    assertThatThrownBy(() -> client.specialties()).isInstanceOf(
      DirectoryUnavailableException.class);
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    server.verify();
  }

  @Test
  void defaultsAndInvalidPropertiesHaveExplicitBounds() {
    assertThat(WebDirectoryResilienceConfiguration.transientFailure(
      new org.springframework.web.client.RestClientException("body",
        new java.net.SocketTimeoutException()))).isTrue();
    assertThat(WebDirectoryResilienceConfiguration.transientFailure(
      new org.springframework.web.client.RestClientException("format",
        new IllegalArgumentException()))).isFalse();
    var properties = new WebDirectoryResilienceProperties(2, Duration.ofMillis(100), 6, 3, 50,
      Duration.ofSeconds(10), 2);
    var config = new WebDirectoryResilienceConfiguration();
    assertThat(config.webDirectoryRetry(properties).getRetryConfig().getMaxAttempts()).isEqualTo(2);
    assertThatThrownBy(
      () -> new WebDirectoryResilienceProperties(0, Duration.ZERO, 6, 3, 50, Duration.ofSeconds(10),
        2)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
      () -> new WebDirectoryResilienceProperties(2, Duration.ZERO, 6, 7, 50, Duration.ofSeconds(10),
        2)).isInstanceOf(IllegalArgumentException.class);
  }
}
