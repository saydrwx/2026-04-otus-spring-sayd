package ru.otus.hw.web.client;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClientException;
import ru.otus.hw.web.dto.PatientDto;
import ru.otus.hw.web.dto.PatientInputDto;
import ru.otus.hw.web.exception.*;

@Component
public class RestPatientClient implements PatientClient {

  private final RestClient http;

  public RestPatientClient(@Qualifier("clinicRestClient") RestClient http) {
    this.http = http;
  }

  @Override
  public Optional<PatientDto> find(String token) {
    try {
      return http.get().uri("/api/patients/me").headers(h -> h.setBearerAuth(token))
        .exchange((request, response) -> {
          if (response.getStatusCode().value() == 404) {
            return Optional.empty();
          }
          check(response.getStatusCode().value(), 200);
          return Optional.of(valid(response.bodyTo(PatientDto.class)));
        });
    } catch (RestClientException exception) {
      throw new ProfileUnavailableException();
    }
  }

  @Override
  public PatientDto create(String token, PatientInputDto input) {
    return write(token, input, true);
  }

  @Override
  public PatientDto update(String token, PatientInputDto input) {
    return write(token, input, false);
  }

  private PatientDto write(String token, PatientInputDto input, boolean create) {
    try {
      RequestBodyUriSpec request = create ? http.post() : http.put();
      return request.uri("/api/patients/me").headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(input).exchange((req, response) -> {
          check(response.getStatusCode().value(), create ? 201 : 200);
          return valid(response.bodyTo(PatientDto.class));
        });
    } catch (RestClientException exception) {
      throw new ProfileUnavailableException();
    }
  }

  private void check(int status, int success) {
    if (status == 401) {
      throw new SessionExpiredException();
    }
    if (status == 403) {
      throw new ProfileForbiddenException();
    }
    if (status == 404) {
      throw new ProfileMissingException();
    }
    if (status == 409) {
      throw new ProfileConflictException();
    }
    if (status == 400) {
      throw new ProfileRejectedException();
    }
    if (status != success) {
      throw new ProfileUnavailableException();
    }
  }

  private PatientDto valid(PatientDto p) {
    if (p == null || p.id() == null || p.firstName() == null || p.firstName().isBlank()
      || p.lastName() == null
      || p.lastName().isBlank() || p.birthDate() == null || !p.birthDate()
      .isBefore(LocalDate.now())) {
      throw new ProfileUnavailableException();
    }
    return p;
  }
}
