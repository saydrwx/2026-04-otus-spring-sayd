package ru.otus.hw.directory.exception;

public class HospitalNotFoundException extends AbstractEntityNotFoundException {

  public HospitalNotFoundException(long id) {
    super("Hospital with id %d was not found".formatted(id));
  }
}
