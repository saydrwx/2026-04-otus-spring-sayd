package ru.otus.hw.web.exception;

public class SessionExpiredException extends RuntimeException {

  public SessionExpiredException() {
    super("SessionExpired");
  }
}
