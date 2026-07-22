package ru.otus.hw.exception;

public abstract class AbstractEntityNotFoundException extends RuntimeException {

  protected AbstractEntityNotFoundException(String message) {
    super(message);
  }
}
