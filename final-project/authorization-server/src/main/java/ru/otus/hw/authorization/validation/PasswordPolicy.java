package ru.otus.hw.authorization.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordPolicyValidator.class)
public @interface PasswordPolicy {

  String message() default "Use 12-64 characters and at most 72 UTF-8 bytes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
