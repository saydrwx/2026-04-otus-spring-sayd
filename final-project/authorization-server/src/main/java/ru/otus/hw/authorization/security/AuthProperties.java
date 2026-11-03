package ru.otus.hw.authorization.security;

import java.net.URI;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
  @NotNull URI issuer,
  @NotBlank String keyStore,
  @NotBlank String keyStorePassword,
  @NotBlank @Size(min = 16) String clientSecret
) { }

