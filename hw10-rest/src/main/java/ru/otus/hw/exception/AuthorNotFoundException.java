package ru.otus.hw.exception;

public final class AuthorNotFoundException extends AbstractEntityNotFoundException {

  public AuthorNotFoundException(long id) {
    super("Author with id %d was not found".formatted(id));
  }
}
