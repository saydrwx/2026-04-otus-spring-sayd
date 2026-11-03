package ru.otus.hw.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportInputDto(
  @NotBlank @Size(max = 10000) String findings,
  @NotBlank @Size(max = 10000) String diagnosis,
  @NotBlank @Size(max = 10000) String recommendations
) {

}
