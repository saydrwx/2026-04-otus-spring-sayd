package ru.otus.hw.web.dto;

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
