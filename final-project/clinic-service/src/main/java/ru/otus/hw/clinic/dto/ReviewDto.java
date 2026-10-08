package ru.otus.hw.clinic.dto;

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

}
