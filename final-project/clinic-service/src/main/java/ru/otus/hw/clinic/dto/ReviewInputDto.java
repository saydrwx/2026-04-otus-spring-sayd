package ru.otus.hw.clinic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewInputDto(
  @Min(1) @Max(5) int rating,
  @Size(max = 2000) String text
) {

}
