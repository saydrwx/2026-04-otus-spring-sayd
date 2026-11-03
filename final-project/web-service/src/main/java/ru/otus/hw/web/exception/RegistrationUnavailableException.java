package ru.otus.hw.web.exception;

public class RegistrationUnavailableException extends RuntimeException {

  public RegistrationUnavailableException() {
    super("RegistrationUnavailable");
  }
}
