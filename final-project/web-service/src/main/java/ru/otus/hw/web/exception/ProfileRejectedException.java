package ru.otus.hw.web.exception;

public class ProfileRejectedException extends RuntimeException {

  public ProfileRejectedException() {
    super("ProfileRejected");
  }
}
