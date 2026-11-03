package ru.otus.hw.clinic.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.otus.hw.clinic.configuration.DocumentProperties;
import ru.otus.hw.clinic.storage.DocumentFileStore;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentCleanupService {

  private final DocumentMetadataService metadata;

  private final DocumentFileStore files;

  private final DocumentProperties properties;

  private final Clock clock;

  public void tryDelete(UUID id) {
    try {
      if (metadata.isPending(id)) {
        files.delete(id);
        metadata.finishDelete(id);
      }
    } catch (RuntimeException exception) {
      log.warn("Document cleanup will retry for {}", id);
    }
  }

  public void failedUpload(UUID id) {
    try {
      if (metadata.failIfStaged(id)) {
        files.delete(id);
        metadata.finishDelete(id);
      }
    } catch (RuntimeException exception) {
      log.warn("Failed upload cleanup will retry for {}", id);
    }
  }

  public void runMaintenance() {
    try {
      Instant cutoff = clock.instant().minus(properties.cleanupGrace());
      for (UUID id : metadata.staleIds(cutoff)) {
        metadata.expireStaged(id, cutoff);
      }
      for (UUID id : metadata.pendingIds()) {
        tryDelete(id);
      }
      for (UUID id : files.oldFileKeys(cutoff)) {
        if (!metadata.exists(id)) {
          files.delete(id);
        }
      }
    } catch (RuntimeException exception) {
      log.warn("Document maintenance could not finish; it will retry");
    }
  }
}
