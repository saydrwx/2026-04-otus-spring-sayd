package ru.otus.hw.web.exception;

public class DoctorNotFoundException extends RuntimeException {

  public DoctorNotFoundException() {
    super("Врач не найден.");
  }
}
