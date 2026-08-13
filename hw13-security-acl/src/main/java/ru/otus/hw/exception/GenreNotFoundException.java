package ru.otus.hw.exception;

public final class GenreNotFoundException extends AbstractEntityNotFoundException {

  public GenreNotFoundException(long id) {
    super(
      "Genre with id %d was not found".formatted(id),
      "exception.genre.not-found",
      id
    );
  }
}
