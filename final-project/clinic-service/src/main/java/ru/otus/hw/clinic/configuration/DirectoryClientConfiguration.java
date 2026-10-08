package ru.otus.hw.clinic.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ru.otus.hw.clinic.exception.DirectoryUnavailableException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.exception.TransientDirectoryException;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DirectoryProperties.class)
public class DirectoryClientConfiguration {

  @Bean
  RestClient directoryRestClient(RestClient.Builder builder, DirectoryProperties properties) {
    var requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(properties.connectTimeout());
    requests.setReadTimeout(properties.readTimeout());
    return builder.baseUrl(properties.baseUrl().toString()).requestFactory(requests).build();
  }

  @Bean
  Retry directoryRetry() {
    return Retry.of("directory", RetryConfig.custom()
      .maxAttempts(2)
      .waitDuration(Duration.ofMillis(100))
      .retryExceptions(TransientDirectoryException.class)
      .build());
  }

  @Bean
  CircuitBreaker directoryCircuitBreaker() {
    return CircuitBreaker.of("directory", CircuitBreakerConfig.custom()
      .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
      .slidingWindowSize(6)
      .minimumNumberOfCalls(3)
      .failureRateThreshold(50)
      .waitDurationInOpenState(Duration.ofSeconds(10))
      .permittedNumberOfCallsInHalfOpenState(2)
      .recordExceptions(DirectoryUnavailableException.class)
      .ignoreExceptions(ResourceNotFoundException.class)
      .build());
  }
}
