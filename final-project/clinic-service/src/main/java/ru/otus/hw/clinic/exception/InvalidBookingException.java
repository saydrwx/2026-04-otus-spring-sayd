package ru.otus.hw.clinic.exception;

public class InvalidBookingException extends RuntimeException {

  public InvalidBookingException(String message) {
    super(message);
  }
}
