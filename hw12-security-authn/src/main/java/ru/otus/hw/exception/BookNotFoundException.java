package ru.otus.hw.exception;

public final class BookNotFoundException extends AbstractEntityNotFoundException {

  public BookNotFoundException(long id) {
    super(
      "Book with id %d was not found".formatted(id),
      "exception.book.not-found",
      id
    );
  }
}
