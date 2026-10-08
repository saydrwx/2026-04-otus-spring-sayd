package ru.otus.hw.web.dto;

import java.util.List;

public record ReportPageDto(
  List<ReportDto> content,
  int page,
  int size,
  long totalElements,
  int totalPages
) {

  @Override
  public String toString() {
    return "ReportPageDto[clinicalData=REDACTED]";
  }
}
