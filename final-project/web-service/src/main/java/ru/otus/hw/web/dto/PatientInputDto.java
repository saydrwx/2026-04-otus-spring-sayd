package ru.otus.hw.web.dto;

import java.time.LocalDate;

public record PatientInputDto(
  String firstName,
  String lastName,
  String middleName,
  LocalDate birthDate
) {

  @Override
  public String toString() {
    return "PatientInputDto[personalData=REDACTED]";
  }
}
