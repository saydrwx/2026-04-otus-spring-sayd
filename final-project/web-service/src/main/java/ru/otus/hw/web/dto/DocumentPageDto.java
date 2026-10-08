package ru.otus.hw.web.dto;

import java.util.List;

public record DocumentPageDto(
  List<DocumentDto> content,
  int page,
  int size,
  long totalElements,
  int totalPages
) {

  @Override
  public String toString() {
    return "DocumentPageDto[documentData=REDACTED]";
  }
}
