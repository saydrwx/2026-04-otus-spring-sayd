package ru.otus.hw.web.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import ru.otus.hw.web.exception.AdminException;

public record StatisticsFilter(LocalDate from, LocalDate to, Long doctorId, Long hospitalId) {

  public StatisticsFilter {

    if (invalidDateRange(from, to) || invalidOptionalId(doctorId) || invalidOptionalId(
      hospitalId)) {
      throw new AdminException(AdminException.Kind.INVALID);
    }
  }

  private static boolean invalidDateRange(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      return true;
    }

    if (!to.isAfter(from) || ChronoUnit.DAYS.between(from, to) > 366) {
      return true;
    }

    return from.isBefore(LocalDate.of(1900, 1, 1))
      || to.isAfter(LocalDate.of(9999, 12, 31));
  }

  private static boolean invalidOptionalId(Long id) {
    return id != null && id <= 0;
  }
}
