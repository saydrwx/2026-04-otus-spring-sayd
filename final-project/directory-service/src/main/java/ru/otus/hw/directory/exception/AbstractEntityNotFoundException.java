package ru.otus.hw.directory.exception;

public abstract class AbstractEntityNotFoundException extends RuntimeException {

  protected AbstractEntityNotFoundException(String message) {
    super(message);
  }
}
