package ru.otus.hw.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PatientDto(
  UUID id,
  String firstName,
  String lastName,
  String middleName,
  LocalDate birthDate
) {

  @Override
  public String toString() {
    return "PatientDto[personalData=REDACTED]";
  }
}
