package ru.otus.hw.web;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.otus.hw.web.client.RestDirectoryClient;
import ru.otus.hw.web.dto.DoctorSearchInput;
import ru.otus.hw.web.exception.DirectoryUnavailableException;
import ru.otus.hw.web.exception.DoctorNotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestDirectoryClientTest {

  MockRestServiceServer server;
  RestDirectoryClient client;

  @BeforeEach
  void setup() {
    var builder = RestClient.builder().baseUrl("http://directory.test");
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RestDirectoryClient(builder.build(),
      new ru.otus.hw.web.resilience.DirectoryReadExecutor(
        io.github.resilience4j.retry.Retry.of("mapping-only",
          io.github.resilience4j.retry.RetryConfig.custom().maxAttempts(1).build()),
        io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("mapping-only")));
  }

  @Test
  void filtersFollowTheExistingDirectoryContractWithoutSendingCredentials() {
    server.expect(
        requestTo("http://directory.test/api/doctors?hospitalId=2&specialtyIds=3&specialtyIds=4"))
      .andExpect(headerDoesNotExist("Authorization"))
      .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    assertThat(client.doctors(new DoctorSearchInput(2L, List.of(3L, 4L)))).isEmpty();
    server.verify();
  }

  @Test
  void hospitalsAndSpecialtiesAreDecoded() {
    server.expect(requestTo("http://directory.test/api/hospitals"))
      .andRespond(withSuccess("[{\"id\":2,\"name\":\"Central\",\"address\":\"Main Street\"}]",
        MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://directory.test/api/specialties"))
      .andRespond(withSuccess("[{\"id\":3,\"name\":\"Cardiology\"}]", MediaType.APPLICATION_JSON));
    assertThat(client.hospitals()).singleElement()
      .satisfies(h -> assertThat(h.name()).isEqualTo("Central"));
    assertThat(client.specialties()).singleElement()
      .satisfies(s -> assertThat(s.name()).isEqualTo("Cardiology"));
    server.verify();
  }

  @Test
  void missingDoctorIsDifferentFromAnOutage() {
    server.expect(requestTo("http://directory.test/api/doctors/99"))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));
    server.expect(requestTo("http://directory.test/api/doctors/1")).andRespond(withServerError());
    assertThatThrownBy(() -> client.doctor(99)).isInstanceOf(DoctorNotFoundException.class);
    assertThatThrownBy(() -> client.doctor(1)).isInstanceOf(DirectoryUnavailableException.class);
    server.verify();
  }

  @Test
  void malformedAndAbsentBodiesDoNotBecomeEmptyResults() {
    server.expect(requestTo("http://directory.test/api/doctors"))
      .andRespond(withSuccess("[{\"id\":1}]", MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://directory.test/api/doctors")).andRespond(withNoContent());
    server.expect(requestTo("http://directory.test/api/doctors"))
      .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> client.doctors(new DoctorSearchInput(null, List.of())))
        .isInstanceOf(DirectoryUnavailableException.class);
    }
    server.verify();
  }
}
