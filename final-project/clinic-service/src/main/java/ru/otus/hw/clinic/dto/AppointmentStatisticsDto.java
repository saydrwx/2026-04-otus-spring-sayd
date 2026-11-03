package ru.otus.hw.clinic.dto;

import java.time.LocalDate;
import java.util.List;

public record AppointmentStatisticsDto(
  LocalDate from,
  LocalDate to,
  String timezone,
  long total,
  List<AppointmentStatisticRow> rows
) {

}
