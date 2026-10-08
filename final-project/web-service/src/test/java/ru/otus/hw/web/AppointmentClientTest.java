package ru.otus.hw.web;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.otus.hw.web.client.RestAppointmentClient;
import ru.otus.hw.web.dto.BookingRequest;
import ru.otus.hw.web.exception.AppointmentsException;
import ru.otus.hw.web.exception.SessionExpiredException;

import static org.assertj.core.api.Assertions.*;
import static ru.otus.hw.web.AppointmentFixtures.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AppointmentClientTest {

  MockRestServiceServer server;
  RestAppointmentClient client;

  @BeforeEach
  void setup() {
    var builder = TestRestClients.builder("http://clinic.test");
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RestAppointmentClient(builder.build());
  }

  @Test
  void publicScheduleAndAvailabilitySendNoCredentials() {
    server.expect(requestTo("http://clinic.test/api/doctors/1/schedule"))
      .andExpect(headerDoesNotExist("Authorization")).andExpect(headerDoesNotExist("Cookie"))
      .andRespond(withSuccess("""
        {"doctorId":1,"hospitalId":2,"timezone":"+03:00","slotDurationMinutes":30,
         "workingDays":[{"dayOfWeek":"THURSDAY","startTime":"09:00:00","endTime":"17:00:00"}]}
        """, MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/doctors/1/availability?date=2030-01-03"))
      .andExpect(headerDoesNotExist("Authorization"))
      .andRespond(withSuccess(AVAILABILITY, MediaType.APPLICATION_JSON));
    assertThat(client.schedule(1).workingDays()).hasSize(1);
    assertThat(client.availability(1, LocalDate.of(2030, 1, 3)).slots()).singleElement().satisfies(
      s -> assertThat(s.start().toInstant()).isEqualTo(Instant.parse("2030-01-03T09:00:00Z")));
    server.verify();
  }

  @Test
  void listAndDetailHaveOnlyTheCurrentBearerAndPreservePaging() {
    server.expect(requestTo("http://clinic.test/api/appointments?page=2&size=20"))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(headerDoesNotExist("Cookie"))
      .andRespond(withSuccess(
        "{\"content\":[" + BODY + "],\"page\":2,\"size\":20,\"totalElements\":41,\"totalPages\":3}",
        MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
    assertThat(client.list("current-token", 2, 20).page()).isEqualTo(2);
    assertThat(client.find("current-token", ID).id()).isEqualTo(ID);
    server.verify();
  }

  @Test
  void bookingSendsOnlyDoctorAndOffsetTimeAndCancellationUsesPost() {
    server.expect(requestTo("http://clinic.test/api/appointments"))
      .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(content().json("{\"doctorId\":1,\"start\":\"2030-01-03T12:00:00+03:00\"}", true))
      .andRespond(
        withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(BODY));
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/cancel"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(headerDoesNotExist("Cookie")).andRespond(withNoContent());
    assertThat(client.book("current-token", new BookingRequest(1, START)).id()).isEqualTo(ID);
    client.cancel("current-token", ID);
    server.verify();
  }

  @Test
  void authenticationAndEligibilityFailuresRetainTheirStatuses() {
    for (var status : java.util.List.of(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN,
      HttpStatus.NOT_FOUND, HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST,
      HttpStatus.SERVICE_UNAVAILABLE)) {
      server.expect(requestTo("http://clinic.test/api/appointments/" + ID))
        .andRespond(withStatus(status).body("private-upstream-details"));
    }
    assertThatThrownBy(() -> client.find("token", ID)).isInstanceOf(SessionExpiredException.class);
    for (var kind : java.util.List.of(AppointmentsException.Kind.FORBIDDEN,
      AppointmentsException.Kind.NOT_FOUND, AppointmentsException.Kind.CONFLICT,
      AppointmentsException.Kind.INVALID, AppointmentsException.Kind.UNAVAILABLE)) {
      assertThatThrownBy(() -> client.find("token", ID)).isInstanceOfSatisfying(
          AppointmentsException.class, e -> assertThat(e.kind()).isEqualTo(kind))
        .hasMessageNotContaining("private-upstream-details");
    }
    server.verify();
  }

  @Test
  void emptyAvailabilityIsSuccessButMissingOrMalformedPayloadIsNot() {
    server.expect(requestTo("http://clinic.test/api/doctors/1/availability?date=2030-01-03"))
      .andRespond(withSuccess(AVAILABILITY.replace(
          "[{\"start\":\"2030-01-03T12:00:00+03:00\",\"end\":\"2030-01-03T12:30:00+03:00\"}]", "[]"),
        MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/doctors/1/availability?date=2030-01-03"))
      .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/doctors/1/availability?date=2030-01-03"))
      .andRespond(withNoContent());
    assertThat(client.availability(1, LocalDate.of(2030, 1, 3)).slots()).isEmpty();
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(
        () -> client.availability(1, LocalDate.of(2030, 1, 3))).isInstanceOfSatisfying(
        AppointmentsException.class,
        e -> assertThat(e.kind()).isEqualTo(AppointmentsException.Kind.UNAVAILABLE));
    }
    server.verify();
  }

  @Test
  void mismatchedIdentifiersDoNotProduceAnAppointmentPage() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + UUID.fromString(
        "3d78918c-3110-4fe8-ad64-c5cbefbaeff0")))
      .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
    assertThatThrownBy(
      () -> client.find("token", UUID.fromString("3d78918c-3110-4fe8-ad64-c5cbefbaeff0")))
      .isInstanceOfSatisfying(AppointmentsException.class,
        e -> assertThat(e.kind()).isEqualTo(AppointmentsException.Kind.UNAVAILABLE));
    server.verify();
  }

  @Test
  void conflictingAndUncertainWritesAreAttemptedOnlyOnce() {
    server.expect(requestTo("http://clinic.test/api/appointments"))
      .andRespond(withStatus(HttpStatus.CONFLICT));
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/cancel"))
      .andRespond(withServerError());
    assertThatThrownBy(() -> client.book("token", new BookingRequest(1, START)))
      .isInstanceOfSatisfying(AppointmentsException.class,
        e -> assertThat(e.kind()).isEqualTo(AppointmentsException.Kind.CONFLICT));
    assertThatThrownBy(() -> client.cancel("token", ID))
      .isInstanceOfSatisfying(AppointmentsException.class,
        e -> assertThat(e.kind()).isEqualTo(AppointmentsException.Kind.UNAVAILABLE));
    server.verify();
  }
}
