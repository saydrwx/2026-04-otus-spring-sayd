package ru.otus.hw.web.service;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.web.client.AppointmentClient;
import ru.otus.hw.web.dto.AppointmentDto;
import ru.otus.hw.web.dto.AppointmentPageDto;
import ru.otus.hw.web.dto.AvailabilityDto;
import ru.otus.hw.web.dto.BookingRequest;
import ru.otus.hw.web.dto.DoctorScheduleDto;

@RequiredArgsConstructor
@Service
public class AppointmentServiceImpl implements AppointmentService {

  private final AppointmentClient client;

  public DoctorScheduleDto schedule(long id) {
    return client.schedule(id);
  }

  public AvailabilityDto availability(long id, LocalDate date) {
    return client.availability(id, date);
  }

  public AppointmentPageDto list(String token, int page, int size) {
    return client.list(token, page, size);
  }

  public AppointmentDto find(String token, UUID id) {
    return client.find(token, id);
  }

  public AppointmentDto book(String token, BookingRequest input) {
    return client.book(token, input);
  }

  public void cancel(String token, UUID id) {
    client.cancel(token, id);
  }
}
