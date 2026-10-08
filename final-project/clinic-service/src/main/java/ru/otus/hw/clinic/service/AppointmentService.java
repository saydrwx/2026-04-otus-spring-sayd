package ru.otus.hw.clinic.service;

import java.util.UUID;
import ru.otus.hw.clinic.dto.AppointmentDto;
import ru.otus.hw.clinic.dto.AppointmentInputDto;
import ru.otus.hw.clinic.dto.AppointmentPageDto;

public interface AppointmentService {

  AppointmentDto book(AppointmentInputDto input);

  AppointmentDto findById(UUID id);

  AppointmentPageDto findCurrent(int page, int size);

  void cancel(UUID id);
}
