package ru.otus.hw.clinic.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AvailabilityDto(
  long doctorId,
  long hospitalId,
  LocalDate date,
  String timezone,
  int slotDurationMinutes,
  OffsetDateTime earliestStart,
  OffsetDateTime latestStart,
  List<SlotDto> slots
) {

}
