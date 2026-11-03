package ru.otus.hw.clinic.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PatientDto(
  UUID id,
  String firstName,
  String lastName,
  String middleName,
  LocalDate birthDate
) {

}
