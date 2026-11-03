package ru.otus.hw.clinic.configuration;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.documents")
public record DocumentProperties(Path root, Duration cleanupGrace) {

  public DocumentProperties {
    if (root == null || cleanupGrace == null || cleanupGrace.isNegative()
      || cleanupGrace.isZero()) {
      throw new IllegalArgumentException("Document root and positive cleanup grace are required");
    }
  }
}
