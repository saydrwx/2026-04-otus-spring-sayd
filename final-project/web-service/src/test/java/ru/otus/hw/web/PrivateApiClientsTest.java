package ru.otus.hw.web;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.otus.hw.web.client.RestRegistrationClient;
import ru.otus.hw.web.client.RestPatientClient;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PrivateApiClientsTest {

  static final String PATIENT = "{\"id\":\"c457b22b-b191-4181-873a-4ec6ec58da22\",\"firstName\":\"Alex\",\"lastName\":\"Smith\",\"middleName\":null,\"birthDate\":\"2000-01-02\"}";

  @Test
  void registrationKeepsCookiesAndCsrfLocalToEachAttempt() {
    var builder = TestRestClients.builder("http://auth.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new RestRegistrationClient(builder.build());
    for (String session : List.of("one", "two")) {
      server.expect(requestTo("http://auth.test/api/csrf")).andExpect(headerDoesNotExist("Cookie"))
        .andRespond(withSuccess(
          "{\"headerName\":\"X-CSRF-TOKEN\",\"parameterName\":\"_csrf\",\"token\":\"masked-"
            + session + "\"}", MediaType.APPLICATION_JSON)
          .header("Set-Cookie", "AUTHSESSION=" + session + "; Path=/; HttpOnly; SameSite=Lax"));
      server.expect(requestTo("http://auth.test/api/registrations"))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header("Cookie", "AUTHSESSION=" + session))
        .andExpect(header("X-CSRF-TOKEN", "masked-" + session))
        .andExpect(headerDoesNotExist("Authorization")).andExpect(
          content().json("{\"email\":\"patient@example.test\",\"password\":\"valid-secret-123\"}",
            true))
        .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(
          "{\"id\":\"c457b22b-b191-4181-873a-4ec6ec58da22\",\"email\":\"patient@example.test\",\"role\":\"PATIENT\"}"));
    }
    client.register(new RegistrationRequest("patient@example.test", "valid-secret-123"));
    client.register(new RegistrationRequest("patient@example.test", "valid-secret-123"));
    server.verify();
  }

  @Test
  void missingCsrfCookiePreventsRegistrationPost() {
    var builder = TestRestClients.builder("http://auth.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new RestRegistrationClient(builder.build());
    server.expect(requestTo("http://auth.test/api/csrf")).andRespond(
      withSuccess("{\"headerName\":\"X-CSRF-TOKEN\",\"token\":\"masked\"}",
        MediaType.APPLICATION_JSON));
    assertThatThrownBy(() -> client.register(
      new RegistrationRequest("p@example.test", "valid-secret-123"))).isInstanceOf(
      RegistrationUnavailableException.class);
    server.verify();
  }

  @Test
  void registrationConflictDoesNotExposeProblemBody() {
    var builder = TestRestClients.builder("http://auth.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new RestRegistrationClient(builder.build());
    server.expect(requestTo("http://auth.test/api/csrf")).andRespond(
      withSuccess("{\"headerName\":\"X-CSRF-TOKEN\",\"token\":\"masked\"}",
        MediaType.APPLICATION_JSON).header("Set-Cookie", "AUTHSESSION=one; Path=/"));
    server.expect(requestTo("http://auth.test/api/registrations"))
      .andRespond(withStatus(HttpStatus.CONFLICT).body("internal-sensitive-provider-detail"));
    assertThatThrownBy(
      () -> client.register(new RegistrationRequest("p@example.test", "valid-secret-123")))
      .isInstanceOf(RegistrationRejectedException.class)
      .hasMessageNotContaining("internal-sensitive-provider-detail");
    server.verify();
  }

  @Test
  void profileMethodsUseOnlyMeAndBearerWithCorrectVerbs() {
    var builder = TestRestClients.builder("http://clinic.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new RestPatientClient(builder.build());
    for (var method : List.of(org.springframework.http.HttpMethod.GET,
      org.springframework.http.HttpMethod.POST, org.springframework.http.HttpMethod.PUT)) {
      var expected = server.expect(requestTo("http://clinic.test/api/patients/me"))
        .andExpect(method(method)).andExpect(header("Authorization", "Bearer server-token"))
        .andExpect(headerDoesNotExist("Cookie"));
      if (method != org.springframework.http.HttpMethod.GET) {
        expected.andExpect(content().json(
          "{\"firstName\":\"Alex\",\"lastName\":\"Smith\",\"middleName\":null,\"birthDate\":\"2000-01-02\"}",
          true));
      }
      expected.andRespond(withStatus(
        method == org.springframework.http.HttpMethod.POST ? HttpStatus.CREATED
          : HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(PATIENT));
    }
    assertThat(client.find("server-token")).isPresent();
    var input = new PatientInputDto("Alex", "Smith", null, LocalDate.of(2000, 1, 2));
    assertThat(client.create("server-token", input).firstName()).isEqualTo("Alex");
    client.update("server-token", input);
    server.verify();
  }

  @Test
  void profileAbsenceIsDifferentFromForbiddenUnauthorizedAndUnavailable() {
    var builder = TestRestClients.builder("http://clinic.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new RestPatientClient(builder.build());
    for (var status : List.of(HttpStatus.NOT_FOUND, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN,
      HttpStatus.SERVICE_UNAVAILABLE)) {
      server.expect(requestTo("http://clinic.test/api/patients/me")).andRespond(withStatus(status));
    }
    assertThat(client.find("token")).isEmpty();
    assertThatThrownBy(() -> client.find("token")).isInstanceOf(SessionExpiredException.class);
    assertThatThrownBy(() -> client.find("token")).isInstanceOf(ProfileForbiddenException.class);
    assertThatThrownBy(() -> client.find("token")).isInstanceOf(ProfileUnavailableException.class);
    server.verify();
  }

  @Test
  void invalidSuccessfulPayloadDoesNotBecomeOnboarding() {
    var builder = TestRestClients.builder("http://clinic.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new RestPatientClient(builder.build());
    server.expect(requestTo("http://clinic.test/api/patients/me"))
      .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    assertThatThrownBy(() -> client.find("token")).isInstanceOf(ProfileUnavailableException.class);
    server.verify();
  }
}
