package ru.otus.hw.web.dto;

import java.util.List;

public record AppointmentPageDto(
  List<AppointmentDto> content,
  int page,
  int size,
  long totalElements,
  int totalPages
) {

  @Override
  public String toString() {
    return "AppointmentPageDto[details=REDACTED]";
  }
}
