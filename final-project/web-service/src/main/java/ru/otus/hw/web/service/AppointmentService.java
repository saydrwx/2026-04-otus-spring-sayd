package ru.otus.hw.web.service;

import java.time.LocalDate;
import java.util.UUID;
import ru.otus.hw.web.dto.AppointmentDto;
import ru.otus.hw.web.dto.AppointmentPageDto;
import ru.otus.hw.web.dto.AvailabilityDto;
import ru.otus.hw.web.dto.BookingRequest;
import ru.otus.hw.web.dto.DoctorScheduleDto;

public interface AppointmentService {

  DoctorScheduleDto schedule(long doctorId);

  AvailabilityDto availability(long doctorId, LocalDate date);

  AppointmentPageDto list(String token, int page, int size);

  AppointmentDto find(String token, UUID id);

  AppointmentDto book(String token, BookingRequest input);

  void cancel(String token, UUID id);
}
