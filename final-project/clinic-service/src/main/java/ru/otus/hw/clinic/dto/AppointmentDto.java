package ru.otus.hw.clinic.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import ru.otus.hw.clinic.model.AppointmentStatus;

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

}
