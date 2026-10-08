package ru.otus.hw.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PatientInputDto(
  @NotBlank @Size(max = 100) String firstName,
  @NotBlank @Size(max = 100) String lastName,
  @Size(max = 100) String middleName,
  @NotNull @Past LocalDate birthDate
) { }
