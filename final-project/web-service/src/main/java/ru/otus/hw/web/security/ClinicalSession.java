package ru.otus.hw.web.security;

import java.util.function.Function;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.otus.hw.web.exception.AppointmentsException;
import ru.otus.hw.web.exception.ClinicalException;
import ru.otus.hw.web.exception.ProfileUnavailableException;
import ru.otus.hw.web.exception.SessionExpiredException;

@RequiredArgsConstructor
@Component
public class ClinicalSession {

  private final SessionAccessTokens tokens;

  public <T> T call(Authentication user, HttpServletRequest request, HttpServletResponse response,
    Function<String, T> action) {
    try {
      return action.apply(tokens.current(user, request, response));
    } catch (SessionExpiredException exception) {
      tokens.expire(user, request, response);
      throw exception;
    } catch (ProfileUnavailableException exception) {
      throw new ClinicalException(ClinicalException.Kind.UNAVAILABLE);
    } catch (AppointmentsException exception) {
      throw new ClinicalException(switch (exception.kind()) {
        case INVALID -> ClinicalException.Kind.INVALID;
        case NOT_FOUND -> ClinicalException.Kind.NOT_FOUND;
        case FORBIDDEN -> ClinicalException.Kind.FORBIDDEN;
        case CONFLICT -> ClinicalException.Kind.CONFLICT;
        case UNAVAILABLE -> ClinicalException.Kind.UNAVAILABLE;
      });
    }
  }
}
