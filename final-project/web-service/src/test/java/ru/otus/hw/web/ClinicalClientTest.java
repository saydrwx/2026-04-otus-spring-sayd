package ru.otus.hw.web;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.otus.hw.web.client.RestClinicalClient;
import ru.otus.hw.web.dto.ReportRequest;
import ru.otus.hw.web.dto.ReviewRequest;
import ru.otus.hw.web.exception.ClinicalException;
import ru.otus.hw.web.exception.SessionExpiredException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static ru.otus.hw.web.AppointmentFixtures.*;
import static ru.otus.hw.web.ClinicalFixtures.*;

class ClinicalClientTest {

  MockRestServiceServer server;
  RestClinicalClient client;

  @BeforeEach
  void setup() {
    var builder = TestRestClients.builder("http://clinic.test");
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RestClinicalClient(builder.build());
  }

  @Test
  void reportAndHistoryReadsUseClinicalPathsAndCurrentBearerWithoutCookies() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/report"))
      .andExpect(method(HttpMethod.GET))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(headerDoesNotExist("Cookie"))
      .andRespond(withSuccess(REPORT_JSON, MediaType.APPLICATION_JSON));
    server.expect(
        requestTo("http://clinic.test/api/patients/" + PATIENT + "/reports?page=1&size=20"))
      .andExpect(header("Authorization", "Bearer current-token")).andRespond(withSuccess("""
        {"content":[%s],"page":1,"size":20,"totalElements":21,"totalPages":2}
        """.formatted(REPORT_JSON), MediaType.APPLICATION_JSON));
    assertThat(client.report("current-token", ID).diagnosis()).isEqualTo("Private diagnosis");
    assertThat(client.reports("current-token", PATIENT, 1, 20).page()).isEqualTo(1);
    server.verify();
  }

  @Test
  void reportSubmissionSendsOnlyFinalReportFieldsAndNoShowUsesItsOwnPost() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/report"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(headerDoesNotExist("Cookie"))
      .andExpect(content().json("""
        {"findings":"Private findings","diagnosis":"Private diagnosis","recommendations":"Private recommendations"}
        """, true)).andRespond(
        withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(REPORT_JSON));
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/no-show"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(header("Authorization", "Bearer current-token")).andRespond(withNoContent());
    client.submitReport("current-token", ID,
      new ReportRequest("Private findings", "Private diagnosis", "Private recommendations"));
    client.noShow("current-token", ID);
    server.verify();
  }

  @Test
  void reviewsUseGetPostPutDeleteWithOnlyRatingAndTextInBodies() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/review"))
      .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer current-token"))
      .andRespond(withSuccess(REVIEW_JSON, MediaType.APPLICATION_JSON));
    for (var method : List.of(HttpMethod.POST, HttpMethod.PUT)) {
      server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/review"))
        .andExpect(method(method)).andExpect(header("Authorization", "Bearer current-token"))
        .andExpect(headerDoesNotExist("Cookie"))
        .andExpect(content().json("""
          {"rating":4,"text":"Review comment"}
          """, true)).andRespond(
          withStatus(method == HttpMethod.POST ? HttpStatus.CREATED : HttpStatus.OK).contentType(
            MediaType.APPLICATION_JSON).body(REVIEW_JSON));
    }
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/review"))
      .andExpect(method(HttpMethod.DELETE))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andRespond(withNoContent());
    assertThat(client.review("current-token", ID)).isPresent();
    client.createReview("current-token", ID, new ReviewRequest(4, "Review comment"));
    client.updateReview("current-token", ID, new ReviewRequest(4, "Review comment"));
    client.deleteReview("current-token", ID);
    server.verify();
  }

  @Test
  void missingReviewIsOptionalButReport404AndOtherErrorsStayErrors() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/review"))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));
    for (var status : List.of(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND,
      HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST, HttpStatus.SERVICE_UNAVAILABLE)) {
      server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/report"))
        .andRespond(withStatus(status).body("private-provider-diagnosis"));
    }
    assertThat(client.review("token", ID)).isEmpty();
    assertThatThrownBy(() -> client.report("token", ID)).isInstanceOf(
      SessionExpiredException.class);
    for (var kind : List.of(ClinicalException.Kind.FORBIDDEN, ClinicalException.Kind.NOT_FOUND,
      ClinicalException.Kind.CONFLICT, ClinicalException.Kind.INVALID,
      ClinicalException.Kind.UNAVAILABLE)) {
      assertThatThrownBy(() -> client.report("token", ID)).isInstanceOfSatisfying(
          ClinicalException.class, e -> assertThat(e.kind()).isEqualTo(kind))
        .hasMessageNotContaining("private-provider-diagnosis");
    }
    server.verify();
  }

  @Test
  void malformedOrMismatchedReportsDoNotBecomeAnEmptyHistoryOrLeakData() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/report"))
      .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    UUID other = UUID.fromString("f0c17bda-dac4-43a5-85b7-1d95d2a011d7");
    server.expect(requestTo("http://clinic.test/api/appointments/" + other + "/report"))
      .andRespond(withSuccess(REPORT_JSON, MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/patients/" + other + "/reports?page=0&size=20"))
      .andRespond(withSuccess("""
        {"content":[%s],"page":0,"size":20,"totalElements":1,"totalPages":1}
        """.formatted(REPORT_JSON), MediaType.APPLICATION_JSON));
    assertUnavailable(() -> client.report("token", ID));
    assertUnavailable(() -> client.report("token", other));
    assertUnavailable(() -> client.reports("token", other, 0, 20));
    server.verify();
  }

  @Test
  void reviewSuccessMustContainAValidRatingAndMatchingAppointment() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/review")).andRespond(
      withSuccess(REVIEW_JSON.replace("\"rating\":4", "\"rating\":6"), MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/review"))
      .andRespond(withNoContent());
    assertUnavailable(() -> client.review("token", ID));
    assertUnavailable(() -> client.review("token", ID));
    server.verify();
  }

  @Test
  void failedClinicalWritesAreSingleAttemptsWithoutReadingProviderBodies() {
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/report"))
      .andRespond(withStatus(HttpStatus.CONFLICT));
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/no-show"))
      .andRespond(withServerError());
    server.expect(requestTo("http://clinic.test/api/appointments/" + ID + "/review"))
      .andRespond(withServerError());
    assertThatThrownBy(() -> client.submitReport("token", ID,
      new ReportRequest("a", "b", "c"))).isInstanceOfSatisfying(ClinicalException.class,
      e -> assertThat(e.kind()).isEqualTo(ClinicalException.Kind.CONFLICT));
    assertUnavailable(() -> {
      client.noShow("token", ID);
      return null;
    });
    assertUnavailable(() -> {
      client.deleteReview("token", ID);
      return null;
    });
    server.verify();
  }

  @Test
  void clinicalDtosDoNotPrintTheirTextInDiagnostics() {
    assertThat(REPORT.toString()).doesNotContain("Private findings", "Private diagnosis");
    assertThat(REVIEW.toString()).doesNotContain("Review comment");
    assertThat(
      new ReportRequest("sensitive-a", "sensitive-b", "sensitive-c").toString()).doesNotContain(
      "sensitive-");
    assertThat(new ReviewRequest(4, "sensitive-comment").toString()).doesNotContain(
      "sensitive-comment");
  }

  void assertUnavailable(java.util.concurrent.Callable<?> action) {
    assertThatThrownBy(action::call).isInstanceOfSatisfying(ClinicalException.class,
      e -> assertThat(e.kind()).isEqualTo(ClinicalException.Kind.UNAVAILABLE));
  }
}
