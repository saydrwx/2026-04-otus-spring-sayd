package ru.otus.hw.clinic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.documents", name = "cleanup-enabled",
  havingValue = "true", matchIfMissing = true)
public class DocumentMaintenance {

  private final DocumentCleanupService cleanup;

  @Scheduled(fixedDelayString = "${app.documents.cleanup-delay:PT1M}",
    initialDelayString = "${app.documents.cleanup-delay:PT1M}")
  public void run() {
    cleanup.runMaintenance();
  }
}
