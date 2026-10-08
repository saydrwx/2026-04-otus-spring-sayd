package ru.otus.hw.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewDto(
  UUID id,
  UUID appointmentId,
  int rating,
  String text,
  Instant createdAt,
  Instant updatedAt
) {

  @Override
  public String toString() {
    return "ReviewDto[clinicalData=REDACTED]";
  }
}
