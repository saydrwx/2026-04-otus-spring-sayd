package ru.otus.hw.clinic.dto;

import ru.otus.hw.clinic.model.AppointmentStatus;

public record AppointmentStatisticRow(
  long doctorId,
  long hospitalId,
  AppointmentStatus status,
  long count
) {

}
