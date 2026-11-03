package ru.otus.hw.directory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record DoctorInputDto(
  @NotBlank @Size(max = 255) String firstName,
  @NotBlank @Size(max = 255) String lastName,
  @Size(max = 1000) String education,
  @NotNull @PositiveOrZero Integer experienceYears,
  @Email @Size(max = 255) String email,
  @NotNull @Positive Long hospitalId,
  @NotEmpty Set<@NotNull @Positive Long> specialtyIds
  ) {

}
