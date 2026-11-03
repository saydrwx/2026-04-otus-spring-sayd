package ru.otus.hw.web.exception;

public class ProfileUnavailableException extends RuntimeException {

  public ProfileUnavailableException() {
    super("ProfileUnavailable");
  }
}
