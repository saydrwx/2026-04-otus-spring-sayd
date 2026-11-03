package ru.otus.hw.clinic.service;

import java.time.LocalDate;
import ru.otus.hw.clinic.dto.AppointmentStatisticsDto;

public interface AppointmentStatisticsService {

  AppointmentStatisticsDto find(
    LocalDate from,
    LocalDate to,
    Long doctorId,
    Long hospitalId
  );
}
