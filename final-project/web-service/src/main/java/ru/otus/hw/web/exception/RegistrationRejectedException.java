package ru.otus.hw.web.exception;

public class RegistrationRejectedException extends RuntimeException {

  public RegistrationRejectedException() {
    super("RegistrationRejected");
  }
}
