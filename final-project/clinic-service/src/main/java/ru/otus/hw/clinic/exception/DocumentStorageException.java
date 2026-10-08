package ru.otus.hw.clinic.exception;

public class DocumentStorageException extends RuntimeException {

  public DocumentStorageException(Throwable cause) {
    super("Document storage is temporarily unavailable", cause);
  }
}
