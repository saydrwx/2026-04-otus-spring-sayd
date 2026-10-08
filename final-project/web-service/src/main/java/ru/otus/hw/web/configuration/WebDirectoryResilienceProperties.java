package ru.otus.hw.web.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.web.directory-resilience")
public record WebDirectoryResilienceProperties(
  int maxAttempts,
  Duration retryWait,
  int windowSize,
  int minimumCalls,
  float failureRate,
  Duration openWait,
  int halfOpenCalls
) {

  public WebDirectoryResilienceProperties {
    if (maxAttempts < 1 || maxAttempts > 3 || retryWait == null || retryWait.isNegative()
      || retryWait.compareTo(Duration.ofSeconds(1)) > 0 || windowSize < 2 || windowSize > 100
      || minimumCalls < 1 || minimumCalls > windowSize || failureRate <= 0 || failureRate > 100
      || !Float.isFinite(failureRate) || openWait == null || openWait.isNegative()
      || openWait.isZero()
      || halfOpenCalls < 1 || halfOpenCalls > 10) {
      throw new IllegalArgumentException("Invalid public directory resilience settings");
    }
  }
}
