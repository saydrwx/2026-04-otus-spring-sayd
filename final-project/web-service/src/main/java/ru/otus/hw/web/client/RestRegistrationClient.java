package ru.otus.hw.web.client;

import java.net.HttpCookie;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.otus.hw.web.dto.RegistrationRequest;
import ru.otus.hw.web.exception.RegistrationRejectedException;
import ru.otus.hw.web.exception.RegistrationUnavailableException;

@Component
public class RestRegistrationClient implements RegistrationClient {

  private final RestClient http;

  public RestRegistrationClient(@Qualifier("registrationRestClient") RestClient http) {
    this.http = http;
  }

  @Override
  public void register(RegistrationRequest input) {
    try {
      CsrfSession csrf = http.get().uri("/api/csrf").exchange((request, response) -> {
        if (response.getStatusCode().value() != 200) {
          throw new RegistrationUnavailableException();
        }
        CsrfResponse body = response.bodyTo(CsrfResponse.class);
        String cookie = sessionCookie(response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE));
        if (body == null || !"X-CSRF-TOKEN".equals(body.headerName()) || body.token() == null
          || body.token().isBlank()
          || body.token().contains("\r") || body.token().contains("\n") || cookie == null) {
          throw new RegistrationUnavailableException();
        }
        return new CsrfSession(body.token(), cookie);
      });
      if (csrf == null) {
        throw new RegistrationUnavailableException();
      }
      http.post().uri("/api/registrations").contentType(MediaType.APPLICATION_JSON)
        .header("X-CSRF-TOKEN", csrf.token()).header(HttpHeaders.COOKIE, csrf.cookie()).body(input)
        .exchange((request, response) -> {
          int status = response.getStatusCode().value();
          if (status == 400 || status == 409) {
            throw new RegistrationRejectedException();
          }
          if (status != 201) {
            throw new RegistrationUnavailableException();
          }
          RegistrationResponse result = response.bodyTo(RegistrationResponse.class);
          if (result == null || result.id() == null || !"PATIENT".equals(result.role())) {
            throw new RegistrationUnavailableException();
          }
          return Boolean.TRUE;
        });
    } catch (RestClientException | IllegalArgumentException exception) {
      throw new RegistrationUnavailableException();
    }
  }

  private String sessionCookie(List<String> headers) {
    for (String header : headers) {
      for (HttpCookie cookie : HttpCookie.parse(header)) {
        if ("AUTHSESSION".equals(cookie.getName()) && cookie.getValue() != null
          && !cookie.getValue().isBlank()
          && cookie.getMaxAge() != 0 && cookie.getValue().matches("[A-Za-z0-9._-]+")) {
          return cookie.getName() + "=" + cookie.getValue();
        }
      }
    }
    return null;
  }

  private record CsrfResponse(String headerName, String parameterName, String token) {

  }

  private record CsrfSession(String token, String cookie) {

    @Override
    public String toString() {
      return "CsrfSession[REDACTED]";
    }
  }

  private record RegistrationResponse(UUID id, String email, String role) {

  }
}
