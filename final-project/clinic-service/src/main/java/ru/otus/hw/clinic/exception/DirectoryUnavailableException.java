package ru.otus.hw.clinic.exception;

public class DirectoryUnavailableException extends RuntimeException {
  public DirectoryUnavailableException(String message) {
    super(message);
  }

  public DirectoryUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
