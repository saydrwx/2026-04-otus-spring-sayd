package ru.otus.hw.clinic.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.clinic.dto.AppointmentStatisticsDto;
import ru.otus.hw.clinic.exception.InvalidBookingException;
import ru.otus.hw.clinic.service.AppointmentStatisticsService;

@RequiredArgsConstructor
@RestController
public class AppointmentStatisticsController {

  private final AppointmentStatisticsService statistics;

  @GetMapping("/api/admin/statistics")
  public ResponseEntity<AppointmentStatisticsDto> find(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @RequestParam(required = false) Long doctorId,
    @RequestParam(required = false) Long hospitalId
  ) {

    if (invalidDateRange(from, to) || invalidOptionalId(doctorId) || invalidOptionalId(
      hospitalId)) {
      throw new InvalidBookingException(
        "Use a positive ID and an exclusive date range of 1 to 366 days");
    }

    return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
      .body(statistics.find(from, to, doctorId, hospitalId));
  }

  private boolean invalidDateRange(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      return true;
    }

    if (!to.isAfter(from) || ChronoUnit.DAYS.between(from, to) > 366) {
      return true;
    }

    return from.isBefore(LocalDate.of(1900, 1, 1))
      || to.isAfter(LocalDate.of(9999, 12, 31));
  }

  private boolean invalidOptionalId(Long id) {
    return id != null && id <= 0;
  }
}
