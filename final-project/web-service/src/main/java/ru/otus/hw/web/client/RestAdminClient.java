package ru.otus.hw.web.client;

import java.util.HashSet;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.*;

import static ru.otus.hw.web.exception.AdminException.Kind.*;

@Component
public class RestAdminClient implements AdminClient {

  private final RestClient directory;

  private final RestClient clinic;

  public RestAdminClient(@Qualifier("adminDirectoryRestClient") RestClient directory,
    @Qualifier("clinicRestClient") RestClient clinic) {
    this.directory = directory;
    this.clinic = clinic;
  }

  @Override
  public long save(String token, CatalogKind kind, Long id, Object input) {
    return call(() -> directory.method(id == null ? HttpMethod.POST : HttpMethod.PUT)
      .uri(id == null ? "/api/" + kind.path() : "/api/" + kind.path() + "/" + id)
      .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON).body(input)
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), id == null ? 201 : 200);
        var body = response.bodyTo(JsonNode.class);
        if (body == null || !body.path("id").isIntegralNumber() || !body.path("id")
          .canConvertToLong()
          || body.path("id").longValue() <= 0 || (id != null
          && body.path("id").longValue() != id)) {
          throw unavailable();
        }
        return body.path("id").longValue();
      }));
  }

  @Override
  public void delete(String token, CatalogKind kind, long id) {
    call(() -> directory.delete().uri("/api/" + kind.path() + "/{id}", id)
      .headers(h -> h.setBearerAuth(token))
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 204);
        return true;
      }));
  }

  @Override
  public AppointmentStatisticsDto statistics(String token, StatisticsFilter filter) {
    return call(() -> clinic.get().uri(b -> {
      b.path("/api/admin/statistics").queryParam("from", filter.from())
        .queryParam("to", filter.to());
      if (filter.doctorId() != null) {
        b.queryParam("doctorId", filter.doctorId());
      }
      if (filter.hospitalId() != null) {
        b.queryParam("hospitalId", filter.hospitalId());
      }
      return b.build();
    }).headers(h -> h.setBearerAuth(token)).exchange((request, response) -> {
      check(response.getStatusCode().value(), 200);
      var value = response.bodyTo(AppointmentStatisticsDto.class);
      if (value == null || !filter.from().equals(value.from()) || !filter.to().equals(value.to())
        || !"UTC+03:00".equals(value.timezone()) || value.rows() == null
        || value.rows().size() > 10000 || value.total() < 0) {
        throw unavailable();
      }
      var keys = new HashSet<String>();
      long total = 0;
      for (var row : value.rows()) {
        if (row == null || row.doctorId() <= 0 || row.hospitalId() <= 0 || row.status() == null
          || row.count() <= 0
          || (filter.doctorId() != null && row.doctorId() != filter.doctorId())
          || (filter.hospitalId() != null && row.hospitalId() != filter.hospitalId())
          || !keys.add(row.doctorId() + ":" + row.hospitalId() + ":" + row.status())) {
          throw unavailable();
        }
        total = Math.addExact(total, row.count());
      }
      if (total != value.total()) {
        throw unavailable();
      }
      return value;
    }));
  }

  private <T> T call(Supplier<T> action) {
    try {
      return action.get();
    } catch (RestClientException | ArithmeticException | IllegalArgumentException exception) {
      throw unavailable();
    }
  }

  private void check(int status, int success) {
    if (status == 401) {
      throw new SessionExpiredException();
    }
    if (status == 400 || status == 422) {
      throw new AdminException(INVALID);
    }
    if (status == 403) {
      throw new AdminException(FORBIDDEN);
    }
    if (status == 404) {
      throw new AdminException(NOT_FOUND);
    }
    if (status == 409) {
      throw new AdminException(CONFLICT);
    }
    if (status != success) {
      throw unavailable();
    }
  }

  private AdminException unavailable() {
    return new AdminException(UNAVAILABLE);
  }
}
