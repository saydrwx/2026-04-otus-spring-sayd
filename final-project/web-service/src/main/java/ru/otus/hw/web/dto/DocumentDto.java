package ru.otus.hw.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(
  UUID id,
  UUID patientId,
  String filename,
  String mediaType,
  long sizeBytes,
  Instant uploadedAt
) {

  @Override
  public String toString() {
    return "DocumentDto[documentData=REDACTED]";
  }
}
