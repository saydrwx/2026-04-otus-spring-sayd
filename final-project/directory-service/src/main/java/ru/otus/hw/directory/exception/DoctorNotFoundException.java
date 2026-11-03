package ru.otus.hw.directory.exception;

public class DoctorNotFoundException extends AbstractEntityNotFoundException {

  public DoctorNotFoundException(long id) {
    super("Doctor with id %d was not found".formatted(id));
  }
}
