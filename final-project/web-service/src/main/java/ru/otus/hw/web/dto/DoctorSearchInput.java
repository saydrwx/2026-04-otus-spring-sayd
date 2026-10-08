package ru.otus.hw.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DoctorSearchInput(
  @Positive Long hospitalId,
  @Size(max = 20) List<@NotNull @Positive Long> specialtyIds) {

  public DoctorSearchInput {
    if (specialtyIds == null) {
      specialtyIds = List.of();
    }
  }
}
