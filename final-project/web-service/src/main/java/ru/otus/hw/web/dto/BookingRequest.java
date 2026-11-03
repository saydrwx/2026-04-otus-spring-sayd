package ru.otus.hw.web.dto;

import java.time.OffsetDateTime;

public record BookingRequest(long doctorId, OffsetDateTime start) {

  @Override
  public String toString() {
    return "BookingRequest[details=REDACTED]";
  }
}
