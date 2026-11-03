package ru.otus.hw.clinic.exception;

public class TransientDirectoryException extends DirectoryUnavailableException {
  public TransientDirectoryException(Throwable cause) {
    super("Directory is temporarily unavailable", cause);
  }
}
