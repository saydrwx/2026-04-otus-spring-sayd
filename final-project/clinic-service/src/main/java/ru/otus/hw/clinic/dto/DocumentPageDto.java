package ru.otus.hw.clinic.dto;

import java.util.List;

public record DocumentPageDto(
  List<DocumentDto> content,
  int page,
  int size,
  long totalElements,
  int totalPages) {

}
