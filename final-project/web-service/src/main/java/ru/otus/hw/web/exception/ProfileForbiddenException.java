package ru.otus.hw.web.exception;

public class ProfileForbiddenException extends RuntimeException {

  public ProfileForbiddenException() {
    super("ProfileForbidden");
  }
}
