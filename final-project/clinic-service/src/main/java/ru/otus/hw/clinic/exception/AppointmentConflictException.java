package ru.otus.hw.clinic.exception;

public class AppointmentConflictException extends RuntimeException {

  public AppointmentConflictException(String message) {
    super(message);
  }
}
