package ru.otus.hw.web.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebDirectoryResilienceProperties.class)
public class WebDirectoryResilienceConfiguration {

  @Bean
  Retry webDirectoryRetry(WebDirectoryResilienceProperties p) {
    return Retry.of("web-public-directory", RetryConfig.custom().maxAttempts(p.maxAttempts())
      .waitDuration(p.retryWait())
      .retryOnException(WebDirectoryResilienceConfiguration::transientFailure).build());
  }

  @Bean
  CircuitBreaker webDirectoryCircuitBreaker(WebDirectoryResilienceProperties p) {
    return CircuitBreaker.of("web-public-directory", CircuitBreakerConfig.custom()
      .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
      .slidingWindowSize(p.windowSize())
      .minimumNumberOfCalls(p.minimumCalls()).failureRateThreshold(p.failureRate())
      .waitDurationInOpenState(p.openWait())
      .permittedNumberOfCallsInHalfOpenState(p.halfOpenCalls())
      .recordException(WebDirectoryResilienceConfiguration::transientFailure)
      .ignoreException(error -> !transientFailure(error)).build());
  }

  public static boolean transientFailure(Throwable error) {
    if (error instanceof ResourceAccessException) {
      return true;
    }
    if (error instanceof RestClientResponseException response) {
      int status = response.getStatusCode().value();
      return status == 500 || status == 502 || status == 503 || status == 504;
    }
    if (error instanceof org.springframework.web.client.RestClientException) {
      Throwable cause = error.getCause();
      for (int depth = 0; cause != null && depth < 10; depth++, cause = cause.getCause()) {
        if (cause instanceof java.net.SocketTimeoutException
          || cause instanceof java.net.SocketException) {
          return true;
        }
      }
    }
    return false;
  }
}
