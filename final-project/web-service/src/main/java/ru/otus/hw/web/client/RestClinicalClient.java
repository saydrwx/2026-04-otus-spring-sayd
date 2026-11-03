package ru.otus.hw.web.client;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.ClinicalException;
import ru.otus.hw.web.exception.SessionExpiredException;

import static ru.otus.hw.web.exception.ClinicalException.Kind.*;

@Component
public class RestClinicalClient implements ClinicalClient {

  private final RestClient http;

  public RestClinicalClient(@Qualifier("clinicRestClient") RestClient http) {
    this.http = http;
  }

  @Override
  public ReportDto report(String token, UUID id) {
    return read(
      () -> http.get().uri("/api/appointments/{id}/report", id).headers(h -> h.setBearerAuth(token))
        .exchange((request, response) -> {
          check(response.getStatusCode().value(), 200);
          return report(response.bodyTo(ReportDto.class), id);
        }));
  }

  @Override
  public ReportPageDto reports(String token, UUID patientId, int page, int size) {
    return read(() -> http.get().uri(
        b -> b.path("/api/patients/{id}/reports").queryParam("page", page).queryParam("size", size)
          .build(patientId))
      .headers(h -> h.setBearerAuth(token)).exchange((request, response) -> {
        check(response.getStatusCode().value(), 200);
        var value = response.bodyTo(ReportPageDto.class);
        if (value == null || value.page() != page || value.size() != size
          || value.totalElements() < 0 || value.totalPages() < 0
          || value.content() == null || value.content().size() > size
          || value.totalElements() < value.content().size()
          || value.totalPages() != (value.totalElements() == 0 ? 0
          : ((value.totalElements() - 1) / size + 1))
          || value.content().stream()
          .anyMatch(r -> !validReport(r) || !patientId.equals(r.patientId()))) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public ReportDto submitReport(String token, UUID id, ReportRequest input) {
    return read(() -> http.post().uri("/api/appointments/{id}/report", id)
      .headers(h -> h.setBearerAuth(token))
      .contentType(MediaType.APPLICATION_JSON).body(input).exchange((request, response) -> {
        check(response.getStatusCode().value(), 201);
        return report(response.bodyTo(ReportDto.class), id);
      }));
  }

  @Override
  public void noShow(String token, UUID id) {
    read(() -> http.post().uri("/api/appointments/{id}/no-show", id)
      .headers(h -> h.setBearerAuth(token))
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 204);
        return Boolean.TRUE;
      }));
  }

  @Override
  public Optional<ReviewDto> review(String token, UUID id) {
    return read(
      () -> http.get().uri("/api/appointments/{id}/review", id).headers(h -> h.setBearerAuth(token))
        .exchange((request, response) -> {
          if (response.getStatusCode().value() == 404) {
            return Optional.empty();
          }
          check(response.getStatusCode().value(), 200);
          return Optional.of(review(response.bodyTo(ReviewDto.class), id));
        }));
  }

  @Override
  public ReviewDto createReview(String token, UUID id, ReviewRequest input) {
    return writeReview(token, id, input, true);
  }

  @Override
  public ReviewDto updateReview(String token, UUID id, ReviewRequest input) {
    return writeReview(token, id, input, false);
  }

  private ReviewDto writeReview(String token, UUID id, ReviewRequest input, boolean create) {
    return read(() -> (create ? http.post() : http.put()).uri("/api/appointments/{id}/review", id)
      .headers(h -> h.setBearerAuth(token))
      .contentType(MediaType.APPLICATION_JSON).body(input).exchange((request, response) -> {
        check(response.getStatusCode().value(), create ? 201 : 200);
        return review(response.bodyTo(ReviewDto.class), id);
      }));
  }

  @Override
  public void deleteReview(String token, UUID id) {
    read(() -> http.delete().uri("/api/appointments/{id}/review", id)
      .headers(h -> h.setBearerAuth(token))
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 204);
        return Boolean.TRUE;
      }));
  }

  private ReportDto report(ReportDto value, UUID id) {
    if (!validReport(value) || !id.equals(value.appointmentId())) {
      throw unavailable();
    }
    return value;
  }

  private boolean validReport(ReportDto r) {
    return r != null && r.id() != null && r.appointmentId() != null && r.patientId() != null
      && r.doctorId() > 0 && r.hospitalId() > 0
      && text(r.findings(), 10000) && text(r.diagnosis(), 10000) && text(r.recommendations(), 10000)
      && r.submittedAt() != null;
  }

  private ReviewDto review(ReviewDto value, UUID id) {
    if (value == null || value.id() == null || !id.equals(value.appointmentId())
      || value.rating() < 1 || value.rating() > 5
      || (value.text() != null && value.text().length() > 2000) || value.createdAt() == null
      || value.updatedAt() == null) {
      throw unavailable();
    }
    return value;
  }

  private boolean text(String value, int max) {
    return value != null && !value.isBlank() && value.length() <= max;
  }

  private <T> T read(Supplier<T> action) {
    try {
      T value = action.get();
      if (value == null) {
        throw unavailable();
      }
      return value;
    } catch (RestClientException exception) {
      throw unavailable();
    }
  }

  private void check(int status, int success) {
    if (status == 401) {
      throw new SessionExpiredException();
    }
    if (status == 400 || status == 422) {
      throw new ClinicalException(INVALID);
    }
    if (status == 403) {
      throw new ClinicalException(FORBIDDEN);
    }
    if (status == 404) {
      throw new ClinicalException(NOT_FOUND);
    }
    if (status == 409) {
      throw new ClinicalException(CONFLICT);
    }
    if (status != success) {
      throw unavailable();
    }
  }

  private ClinicalException unavailable() {
    return new ClinicalException(UNAVAILABLE);
  }
}
