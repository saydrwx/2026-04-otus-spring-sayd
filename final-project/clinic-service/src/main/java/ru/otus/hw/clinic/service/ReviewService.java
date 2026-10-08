package ru.otus.hw.clinic.service;

import java.util.UUID;
import ru.otus.hw.clinic.dto.ReviewDto;
import ru.otus.hw.clinic.dto.ReviewInputDto;

public interface ReviewService {

  ReviewDto findByAppointmentId(UUID appointmentId);

  ReviewDto create(UUID appointmentId, ReviewInputDto input);

  ReviewDto update(UUID appointmentId, ReviewInputDto input);

  void delete(UUID appointmentId);
}
