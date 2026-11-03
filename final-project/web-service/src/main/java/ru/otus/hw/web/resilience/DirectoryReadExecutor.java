package ru.otus.hw.web.resilience;

import java.util.function.Supplier;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.otus.hw.web.exception.DirectoryUnavailableException;

@Slf4j
@Component
public class DirectoryReadExecutor {

  private final Retry retry;

  private final CircuitBreaker breaker;

  public DirectoryReadExecutor(@Qualifier("webDirectoryRetry") Retry retry,
    @Qualifier("webDirectoryCircuitBreaker") CircuitBreaker breaker) {
    this.retry = retry;
    this.breaker = breaker;
    breaker.getEventPublisher().onStateTransition(
      event -> log.info("Public directory circuit {}", event.getStateTransition()));
    retry.getEventPublisher().onRetry(
      event -> log.info("Public directory read retry {}", event.getNumberOfRetryAttempts()));
  }

  public <T> T execute(Supplier<T> read) {
    try {
      return breaker.executeSupplier(Retry.decorateSupplier(retry, read));
    } catch (CallNotPermittedException exception) {
      throw new DirectoryUnavailableException();
    }
  }
}
