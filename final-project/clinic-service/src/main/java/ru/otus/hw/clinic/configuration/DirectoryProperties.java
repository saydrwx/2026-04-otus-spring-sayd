package ru.otus.hw.clinic.configuration;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.directory")
public record DirectoryProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {

  public DirectoryProperties {
    if (baseUrl == null || baseUrl.getHost() == null
      || !("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))) {
      throw new IllegalArgumentException("Directory base URL must be an absolute HTTP(S) URL");
    }
    if (connectTimeout == null || !connectTimeout.isPositive() || readTimeout == null
      || !readTimeout.isPositive()) {
      throw new IllegalArgumentException("Directory timeouts must be positive");
    }
  }
}
