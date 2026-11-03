package ru.otus.hw.web;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.otus.hw.web.client.RestAdminClient;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AdminClientTest {

  MockRestServiceServer directory, clinic;
  RestAdminClient client;
  final StatisticsFilter filter = new StatisticsFilter(LocalDate.of(2030, 1, 1),
    LocalDate.of(2030, 1, 3), 1L, 2L);
  static final String STATS = """
    {"from":"2030-01-01","to":"2030-01-03","timezone":"UTC+03:00","total":2,
    "rows":[{"doctorId":1,"hospitalId":2,"status":"COMPLETED","count":2}]}
    """;

  @BeforeEach
  void setup() {
    var db = TestRestClients.builder("http://directory.test");
    directory = MockRestServiceServer.bindTo(db).build();
    var cb = TestRestClients.builder("http://clinic.test");
    clinic = MockRestServiceServer.bindTo(cb).build();
    client = new RestAdminClient(db.build(), cb.build());
  }

  @Test
  void catalogUsesExactVerbsBearerAndOnlyInputFields() {
    directory.expect(requestTo("http://directory.test/api/hospitals"))
      .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer server-token"))
      .andExpect(headerDoesNotExist("Cookie"))
      .andExpect(content().json("{\"name\":\"Clinic\",\"address\":\"Street\"}"))
      .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED).contentType(
        MediaType.APPLICATION_JSON).body("{\"id\":7}"));
    directory.expect(requestTo("http://directory.test/api/specialties/3"))
      .andExpect(method(HttpMethod.PUT)).andExpect(header("Authorization", "Bearer server-token"))
      .andExpect(content().json("{\"name\":\"Cardiology\"}"))
      .andRespond(withSuccess("{\"id\":3}", MediaType.APPLICATION_JSON));
    directory.expect(requestTo("http://directory.test/api/doctors/1"))
      .andExpect(method(HttpMethod.DELETE))
      .andExpect(header("Authorization", "Bearer server-token")).andRespond(withNoContent());
    assertThat(client.save("server-token", CatalogKind.HOSPITALS, null,
      new HospitalAdminForm.Input("Clinic", "Street"))).isEqualTo(7);
    client.save("server-token", CatalogKind.SPECIALTIES, 3L,
      new SpecialtyAdminForm.Input("Cardiology"));
    client.delete("server-token", CatalogKind.DOCTORS, 1);
    directory.verify();
    clinic.verify();
  }

  @Test
  void statisticsUsesExplicitDateAndIdFiltersWithNoDirectoryRead() {
    clinic.expect(requestTo(
        "http://clinic.test/api/admin/statistics?from=2030-01-01&to=2030-01-03&doctorId=1&hospitalId=2"))
      .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer server-token"))
      .andExpect(headerDoesNotExist("Cookie"))
      .andRespond(withSuccess(STATS, MediaType.APPLICATION_JSON));
    var value = client.statistics("server-token", filter);
    assertThat(value.total()).isEqualTo(2);
    assertThat(value.rows().getFirst().hospitalId()).isEqualTo(2);
    clinic.verify();
    directory.verify();
  }

  @Test
  void providerErrorsAreSanitizedAndMutationsAreNotRetried() {
    directory.expect(requestTo("http://directory.test/api/doctors/1")).andRespond(
      withStatus(org.springframework.http.HttpStatus.CONFLICT).body(
        "secret table_name stacktrace"));
    assertThatThrownBy(
      () -> client.delete("server-token", CatalogKind.DOCTORS, 1)).isInstanceOfSatisfying(
      AdminException.class, e -> {
        assertThat(e.kind()).isEqualTo(AdminException.Kind.CONFLICT);
        assertThat(e.getMessage()).doesNotContain("secret", "table_name");
      });
    directory.verify();
  }

  @Test
  void unauthorizedIsSessionExpiryWithoutReplay() {
    directory.expect(requestTo("http://directory.test/api/hospitals/2"))
      .andRespond(withUnauthorizedRequest());
    assertThatThrownBy(() -> client.delete("server-token", CatalogKind.HOSPITALS, 2)).isInstanceOf(
      SessionExpiredException.class);
    directory.verify();
  }

  @Test
  void malformedOrMismatchedSaveResponseIsUnconfirmed() {
    directory.expect(requestTo("http://directory.test/api/specialties/3"))
      .andRespond(withSuccess("{\"id\":4}", MediaType.APPLICATION_JSON));
    assertThatThrownBy(() -> client.save("server-token", CatalogKind.SPECIALTIES, 3L,
      new SpecialtyAdminForm.Input("Name"))).isInstanceOf(AdminException.class);
    directory.verify();
  }

  @Test
  void inconsistentStatisticsNeverReachCsv() {
    for (String body : List.of(STATS.replace("\"total\":2", "\"total\":3"),
      STATS.replace("\"hospitalId\":2", "\"hospitalId\":8"),
      STATS.replace("\"count\":2", "\"count\":-1"), STATS.replace("2030-01-03", "2030-01-04"))) {
      setup();
      clinic.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
      assertThatThrownBy(() -> client.statistics("server-token", filter)).isInstanceOfSatisfying(
        AdminException.class, e -> assertThat(e.kind()).isEqualTo(AdminException.Kind.UNAVAILABLE));
      clinic.verify();
    }
  }

  @Test
  void emptyResultIsValidAndUpstreamRedirectIsUnavailable() {
    clinic.expect(anything()).andRespond(withSuccess(STATS.replace("\"total\":2", "\"total\":0")
        .replace("[{\"doctorId\":1,\"hospitalId\":2,\"status\":\"COMPLETED\",\"count\":2}]", "[]"),
      MediaType.APPLICATION_JSON));
    assertThat(client.statistics("server-token", filter).total()).isZero();
    clinic.verify();
    directory.expect(anything()).andRespond(
      withStatus(org.springframework.http.HttpStatus.FOUND).header("Location",
        "https://other.test"));
    assertThatThrownBy(() -> client.delete("server-token", CatalogKind.DOCTORS, 1)).isInstanceOf(
      AdminException.class);
    directory.verify();
  }
}
