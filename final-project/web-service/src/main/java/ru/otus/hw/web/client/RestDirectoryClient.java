package ru.otus.hw.web.client;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.DirectoryUnavailableException;
import ru.otus.hw.web.exception.DoctorNotFoundException;
import ru.otus.hw.web.resilience.DirectoryReadExecutor;

@Component
public class RestDirectoryClient implements DirectoryClient {

  private final RestClient http;

  private final DirectoryReadExecutor reads;

  public RestDirectoryClient(@Qualifier("directoryRestClient") RestClient http,
    ru.otus.hw.web.resilience.DirectoryReadExecutor reads) {
    this.http = http;
    this.reads = reads;
  }

  @Override
  public List<HospitalDto> hospitals() {
    var values = read(() -> http.get().uri("/api/hospitals").retrieve()
      .body(new ParameterizedTypeReference<List<HospitalDto>>() {
      }));
    if (values.stream().anyMatch(h -> h == null || h.id() <= 0 || !text(h.name()))) {
      throw unavailable();
    }
    return values;
  }

  @Override
  public List<SpecialtyDto> specialties() {
    var values = read(() -> http.get().uri("/api/specialties").retrieve()
      .body(new ParameterizedTypeReference<List<SpecialtyDto>>() {
      }));
    if (values.stream().anyMatch(s -> s == null || s.id() <= 0 || !text(s.name()))) {
      throw unavailable();
    }
    return values;
  }

  @Override
  public List<DoctorDto> doctors(DoctorSearchInput input) {
    var values = read(() -> http.get().uri(builder -> {
      builder.path("/api/doctors");
      if (input.hospitalId() != null) {
        builder.queryParam("hospitalId", input.hospitalId());
      }
      for (Long id : input.specialtyIds()) {
        builder.queryParam("specialtyIds", id);
      }
      return builder.build();
    }).retrieve().body(new ParameterizedTypeReference<List<DoctorDto>>() {
    }));
    if (values.stream().anyMatch(d -> !valid(d))) {
      throw unavailable();
    }
    return values;
  }

  @Override
  public DoctorDto doctor(long id) {
    try {
      var value = reads.execute(
        () -> http.get().uri("/api/doctors/{id}", id).retrieve().body(DoctorDto.class));
      if (!valid(value) || value.id() != id) {
        throw unavailable();
      }
      return value;
    } catch (HttpClientErrorException.NotFound exception) {
      throw new DoctorNotFoundException();
    } catch (RestClientException exception) {
      throw unavailable();
    }
  }

  private <T> T read(Supplier<T> request) {
    try {
      T value = reads.execute(request);
      if (value == null) {
        throw unavailable();
      }
      return value;
    } catch (RestClientException exception) {
      throw unavailable();
    }
  }

  private boolean valid(DoctorDto d) {
    return d != null && d.id() > 0 && text(d.firstName()) && text(d.lastName())
      && d.hospital() != null
      && d.hospital().id() > 0 && text(d.hospital().name()) && d.specialties() != null
      && d.specialties().stream().allMatch(s -> s != null && s.id() > 0 && text(s.name()));
  }

  private boolean text(String value) {
    return value != null && !value.isBlank();
  }

  private DirectoryUnavailableException unavailable() {
    return new DirectoryUnavailableException();
  }
}
