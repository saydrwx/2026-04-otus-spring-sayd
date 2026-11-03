package ru.otus.hw.web.client;

import java.time.LocalDate;
import java.util.UUID;
import ru.otus.hw.web.dto.*;

public interface AppointmentClient {

  DoctorScheduleDto schedule(long doctorId);

  AvailabilityDto availability(long doctorId, LocalDate date);

  AppointmentPageDto list(String token, int page, int size);

  AppointmentDto find(String token, UUID id);

  AppointmentDto book(String token, BookingRequest input);

  void cancel(String token, UUID id);
}
