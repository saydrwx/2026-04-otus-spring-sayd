package ru.otus.hw.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordPolicyValidator implements ConstraintValidator<PasswordPolicy, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (value.codePoints().anyMatch(cp -> cp >= 0xD800 && cp <= 0xDFFF)) {
      return false;
    }
    int characters = value.codePointCount(0, value.length());
    return characters >= 12 && characters <= 64
      && value.getBytes(StandardCharsets.UTF_8).length <= 72;
  }
}
