package ru.otus.hw.clinic.service;

import java.time.LocalDate;
import ru.otus.hw.clinic.dto.AvailabilityDto;

public interface AvailabilityService {

  AvailabilityDto findByDoctorId(long doctorId, LocalDate date);
}
