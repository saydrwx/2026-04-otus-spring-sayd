package ru.otus.hw.clinic.exception;

import org.springframework.http.HttpStatus;

public class InvalidDocumentException extends RuntimeException {

  private final HttpStatus status;

  public InvalidDocumentException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
