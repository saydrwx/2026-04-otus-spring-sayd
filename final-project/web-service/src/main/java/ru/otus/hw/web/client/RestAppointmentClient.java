package ru.otus.hw.web.client;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.AppointmentsException;
import ru.otus.hw.web.exception.SessionExpiredException;

import static ru.otus.hw.web.exception.AppointmentsException.Kind.*;

@Component
public class RestAppointmentClient implements AppointmentClient {

  private final RestClient http;

  public RestAppointmentClient(@Qualifier("clinicRestClient") RestClient http) {
    this.http = http;
  }

  @Override
  public DoctorScheduleDto schedule(long doctorId) {
    return read(
      () -> http.get().uri("/api/doctors/{id}/schedule", doctorId).exchange((request, response) -> {
        check(response.getStatusCode().value(), 200);
        var value = response.bodyTo(DoctorScheduleDto.class);
        if (value == null || value.doctorId() != doctorId || value.hospitalId() <= 0 || !zone(
          value.timezone())
          || value.slotDurationMinutes() <= 0 || value.workingDays() == null || value.workingDays()
          .stream().anyMatch(d -> d == null
            || d.dayOfWeek() == null || d.startTime() == null || d.endTime() == null || !d.endTime()
            .isAfter(d.startTime()))) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public AvailabilityDto availability(long doctorId, LocalDate date) {
    return read(() -> http.get()
      .uri(b -> b.path("/api/doctors/{id}/availability").queryParam("date", date).build(doctorId))
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 200);
        var value = response.bodyTo(AvailabilityDto.class);
        if (value == null || value.doctorId() != doctorId || value.hospitalId() <= 0
          || !date.equals(value.date()) || !zone(value.timezone())
          || value.slotDurationMinutes() <= 0 || value.earliestStart() == null
          || value.latestStart() == null
          || value.latestStart().toInstant().isBefore(value.earliestStart().toInstant())
          || value.slots() == null
          || value.slots().stream().anyMatch(
          s -> s == null || s.start() == null || s.end() == null || !s.end().toInstant()
            .isAfter(s.start().toInstant())
            || s.start().toInstant().isBefore(value.earliestStart().toInstant()) || s.start()
            .toInstant().isAfter(value.latestStart().toInstant())
            || !java.time.Duration.between(s.start(), s.end())
            .equals(java.time.Duration.ofMinutes(value.slotDurationMinutes()))
            || !s.start().atZoneSameInstant(ZoneId.of(value.timezone())).toLocalDate()
            .equals(date))) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public AppointmentPageDto list(String token, int page, int size) {
    return read(() -> http.get().uri(
        b -> b.path("/api/appointments").queryParam("page", page).queryParam("size", size).build())
      .headers(h -> h.setBearerAuth(token)).exchange((request, response) -> {
        check(response.getStatusCode().value(), 200);
        var value = response.bodyTo(AppointmentPageDto.class);
        if (value == null || value.page() != page || value.size() != size
          || value.totalElements() < 0 || value.totalPages() < 0
          || value.content() == null || value.totalElements() < value.content().size()
          || value.totalPages() != (value.totalElements() == 0 ? 0
          : ((value.totalElements() - 1) / size + 1)) || value.content().size() > size
          || value.content().stream().anyMatch(a -> !valid(a))) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public AppointmentDto find(String token, UUID id) {
    return read(
      () -> http.get().uri("/api/appointments/{id}", id).headers(h -> h.setBearerAuth(token))
        .exchange((request, response) -> {
          check(response.getStatusCode().value(), 200);
          var value = response.bodyTo(AppointmentDto.class);
          if (!valid(value) || !id.equals(value.id())) {
            throw unavailable();
          }
          return value;
        }));
  }

  @Override
  public AppointmentDto book(String token, BookingRequest input) {
    return read(() -> http.post().uri("/api/appointments").headers(h -> h.setBearerAuth(token))
      .contentType(MediaType.APPLICATION_JSON).body(input)
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 201);
        var value = response.bodyTo(AppointmentDto.class);
        if (!valid(value) || value.doctorId() != input.doctorId() || !value.start().toInstant()
          .equals(input.start().toInstant())) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public void cancel(String token, UUID id) {
    read(() -> http.post().uri("/api/appointments/{id}/cancel", id)
      .headers(h -> h.setBearerAuth(token))
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 204);
        return Boolean.TRUE;
      }));
  }

  private <T> T read(Supplier<T> action) {
    try {
      T result = action.get();
      if (result == null) {
        throw unavailable();
      }
      return result;
    } catch (RestClientException exception) {
      throw unavailable();
    }
  }

  private void check(int status, int success) {
    if (status == 401) {
      throw new SessionExpiredException();
    }
    if (status == 400 || status == 422) {
      throw new AppointmentsException(INVALID);
    }
    if (status == 403) {
      throw new AppointmentsException(FORBIDDEN);
    }
    if (status == 404) {
      throw new AppointmentsException(NOT_FOUND);
    }
    if (status == 409) {
      throw new AppointmentsException(CONFLICT);
    }
    if (status != success) {
      throw unavailable();
    }
  }

  private boolean zone(String value) {
    try {
      return value != null && java.time.ZoneOffset.of(value)
        .equals(ru.otus.hw.web.view.ClinicTimes.OFFSET);
    } catch (java.time.DateTimeException exception) {
      return false;
    }
  }

  private boolean valid(AppointmentDto value) {
    return value != null && value.id() != null && value.patientId() != null && value.doctorId() > 0
      && value.hospitalId() > 0
      && value.start() != null && value.end() != null && value.end().toInstant()
      .isAfter(value.start().toInstant())
      && value.status() != null && value.bookedAt() != null;
  }

  private AppointmentsException unavailable() {
    return new AppointmentsException(UNAVAILABLE);
  }
}
