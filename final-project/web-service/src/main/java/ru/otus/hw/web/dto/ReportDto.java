package ru.otus.hw.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportDto(
  UUID id,
  UUID appointmentId,
  UUID patientId,
  long doctorId,
  long hospitalId,
  String findings,
  String diagnosis,
  String recommendations,
  Instant submittedAt
) {

  @Override
  public String toString() {
    return "ReportDto[clinicalData=REDACTED]";
  }
}
