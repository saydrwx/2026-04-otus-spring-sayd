package ru.otus.hw.web.configuration;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.web")
public record WebProperties(
  URI publicBaseUrl,
  URI issuer,
  URI authBackendBaseUrl,
  URI directoryBaseUrl,
  URI clinicBaseUrl,
  String clientSecret,
  Duration connectTimeout,
  Duration readTimeout,
  Duration documentReadTimeout
) {

  public WebProperties {
    for (URI uri : new URI[]{publicBaseUrl, issuer, authBackendBaseUrl, directoryBaseUrl,
      clinicBaseUrl}) {
      if (uri == null || uri.getHost() == null || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null || !("http".equals(uri.getScheme()) || "https".equals(
        uri.getScheme()))
        || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
        throw new IllegalArgumentException(
          "Web/auth/directory base URLs must be absolute HTTP(S) origins");
      }
    }
    if (clientSecret == null || clientSecret.length() < 16) {
      throw new IllegalArgumentException("Set CLIENT_SECRET to the existing hospital-web secret");
    }
    if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
      || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()
      || documentReadTimeout == null || documentReadTimeout.isZero()
      || documentReadTimeout.isNegative()) {
      throw new IllegalArgumentException("Positive HTTP timeouts are required");
    }
  }

  @Override
  public String toString() {
    return "WebProperties[credentials=REDACTED]";
  }
}
