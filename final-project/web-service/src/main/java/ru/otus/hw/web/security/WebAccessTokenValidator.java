package ru.otus.hw.web.security;

import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class WebAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    try {
      if (hasValidTimestamps(token) && hasRequiredAudience(token) && hasCanonicalUuidSubject(
        token)) {
        return OAuth2TokenValidatorResult.success();
      }
    } catch (IllegalArgumentException ignored) {
      System.out.println("IllegalArgumentException ignored");
    }
    return OAuth2TokenValidatorResult.failure(
      new OAuth2Error("invalid_token", "Invalid account token", null));
  }

  private static boolean hasValidTimestamps(Jwt token) {
    return token.getExpiresAt() != null
      && token.getIssuedAt() != null
      && token.getExpiresAt().isAfter(token.getIssuedAt());
  }

  private static boolean hasRequiredAudience(Jwt token) {
    return token.getAudience() != null
      && token.getAudience().contains("hospital-api");
  }

  private static boolean hasCanonicalUuidSubject(Jwt token) {
    return token.getSubject() != null
      && UUID.fromString(token.getSubject()).toString().equals(token.getSubject());
  }
}
