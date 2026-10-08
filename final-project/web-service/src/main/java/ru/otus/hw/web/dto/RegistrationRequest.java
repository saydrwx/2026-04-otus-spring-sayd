package ru.otus.hw.web.dto;

public record RegistrationRequest(String email, String password) {

  @Override
  public String toString() {
    return "RegistrationRequest[credentials=REDACTED]";
  }
}
