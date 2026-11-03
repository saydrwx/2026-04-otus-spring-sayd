package ru.otus.hw.clinic.dto;

import java.util.List;

public record DoctorScheduleDto(
  long doctorId,
  long hospitalId,
  String timezone,
  int slotDurationMinutes,
  List<WorkingDayDto> workingDays
) {

}
