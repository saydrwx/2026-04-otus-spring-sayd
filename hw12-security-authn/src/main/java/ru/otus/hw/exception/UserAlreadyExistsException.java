package ru.otus.hw.exception;

import lombok.Getter;

@Getter
public final class UserAlreadyExistsException extends RuntimeException {

  private final String userName;

  public UserAlreadyExistsException(String userName) {
    super("User with name %s already exists".formatted(userName));
    this.userName = userName;
  }

  public UserAlreadyExistsException(String userName, Throwable cause) {
    super("User with name %s already exists".formatted(userName), cause);
    this.userName = userName;
  }
}
