package ru.otus.hw.clinic.dto;

import java.util.List;

public record ReportPageDto(
  List<ReportDto> content,
  int page,
  int size,
  long totalElements,
  int totalPages
) {

}
