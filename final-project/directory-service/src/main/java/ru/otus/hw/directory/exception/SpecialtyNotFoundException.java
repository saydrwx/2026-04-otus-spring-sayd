package ru.otus.hw.directory.exception;

public class SpecialtyNotFoundException extends AbstractEntityNotFoundException {

  public SpecialtyNotFoundException(long id) {
    super("Specialty with id %d was not found".formatted(id));
  }
}
