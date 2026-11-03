package ru.otus.hw.web.dto;

public record AppointmentStatisticRow(
  long doctorId,
  long hospitalId,
  AppointmentStatus status,
  long count
) {

}
