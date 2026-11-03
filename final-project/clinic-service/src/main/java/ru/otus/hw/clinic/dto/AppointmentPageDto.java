package ru.otus.hw.clinic.dto;

import java.util.List;

public record AppointmentPageDto(
  List<AppointmentDto> content,
  int page,
  int size,
  long totalElements,
  int totalPages
) {

}
