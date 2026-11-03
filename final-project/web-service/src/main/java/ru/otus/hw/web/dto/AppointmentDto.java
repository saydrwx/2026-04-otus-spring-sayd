package ru.otus.hw.web.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;


public record AppointmentDto(
  UUID id,
  UUID patientId,
  long doctorId,
  long hospitalId,
  OffsetDateTime start,
  OffsetDateTime end,
  AppointmentStatus status,
  Instant bookedAt,
  Instant cancelledAt,
  Instant outcomeAt
) {

  @Override
  public String toString() {
    return "AppointmentDto[details=REDACTED]";
  }
}
