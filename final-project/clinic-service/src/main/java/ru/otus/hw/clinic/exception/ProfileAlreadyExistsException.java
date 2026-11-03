package ru.otus.hw.clinic.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
  public ProfileAlreadyExistsException(String message) {
    super(message);
  }
}
