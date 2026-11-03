package ru.otus.hw.web.exception;

public class ProfileConflictException extends RuntimeException {

  public ProfileConflictException() {
    super("ProfileConflict");
  }
}
