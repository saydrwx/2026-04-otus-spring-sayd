package ru.otus.hw.clinic.security;

import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AccountTokenValidator implements OAuth2TokenValidator<Jwt> {

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    boolean validSubject = false;
    try {
      String subject = jwt.getSubject();
      validSubject = subject != null && UUID.fromString(subject).toString().equals(subject);
    } catch (IllegalArgumentException ignored) {
      System.out.println("IllegalArgumentException ignored");
    }
    return validSubject && jwt.getAudience() != null && jwt.getAudience().contains("hospital-api")
      ? OAuth2TokenValidatorResult.success()
      : OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "Invalid audience or account subject", null));
  }
}
