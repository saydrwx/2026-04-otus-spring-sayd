package ru.otus.hw.web.exception;

public class ProfileMissingException extends RuntimeException {

  public ProfileMissingException() {
    super("ProfileMissing");
  }
}
