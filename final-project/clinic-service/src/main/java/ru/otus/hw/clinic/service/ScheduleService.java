package ru.otus.hw.clinic.service;

import ru.otus.hw.clinic.dto.DoctorScheduleDto;

public interface ScheduleService {

  DoctorScheduleDto findByDoctorId(long doctorId);
}
