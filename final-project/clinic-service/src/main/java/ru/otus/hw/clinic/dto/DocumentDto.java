package ru.otus.hw.clinic.dto;

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

}
