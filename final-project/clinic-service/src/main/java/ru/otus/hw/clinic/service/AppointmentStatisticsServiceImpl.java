package ru.otus.hw.clinic.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.dto.AppointmentStatisticsDto;
import ru.otus.hw.clinic.exception.InvalidBookingException;
import ru.otus.hw.clinic.repository.AppointmentStatisticsRepository;

@RequiredArgsConstructor
@Service
public class AppointmentStatisticsServiceImpl implements AppointmentStatisticsService {

  private final AppointmentStatisticsRepository repository;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional(readOnly = true)
  public AppointmentStatisticsDto find(
    LocalDate from,
    LocalDate to,
    Long doctorId,
    Long hospitalId
  ) {
    var offset = ZoneOffset.ofHours(3);
    var rows = repository.aggregate(from.atStartOfDay().atOffset(offset),
      to.atStartOfDay().atOffset(offset),
      doctorId, hospitalId, PageRequest.of(0, 10001));
    if (rows.size() > 10000) {
      throw new InvalidBookingException("Narrow the date range or select a doctor or hospital");
    }
    long total = 0;
    for (var row : rows) {
      total = Math.addExact(total, row.count());
    }
    return new AppointmentStatisticsDto(from, to, "UTC+03:00", total, java.util.List.copyOf(rows));
  }
}
