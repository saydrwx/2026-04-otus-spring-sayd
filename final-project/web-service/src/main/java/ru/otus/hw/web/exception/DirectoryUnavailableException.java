package ru.otus.hw.web.exception;

public class DirectoryUnavailableException extends RuntimeException {

  public DirectoryUnavailableException() {
    super("Справочник временно недоступен. Повторите попытку позже.");
  }
}
