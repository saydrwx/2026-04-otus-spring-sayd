package ru.otus.hw.exception;

import lombok.Getter;

public abstract class AbstractEntityNotFoundException extends RuntimeException {

  @Getter
  private final String messageCode;

  private final Object[] messageArguments;

  protected AbstractEntityNotFoundException(
    String diagnosticMessage,
    String messageCode,
    Object... messageArguments
  ) {
    super(diagnosticMessage);
    this.messageCode = messageCode;
    this.messageArguments = messageArguments.clone();
  }

  public Object[] getMessageArguments() {
    return messageArguments.clone();
  }
}
