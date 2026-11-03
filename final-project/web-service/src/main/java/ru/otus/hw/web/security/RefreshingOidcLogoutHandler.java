package ru.otus.hw.web.security;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import ru.otus.hw.web.exception.ProfileUnavailableException;
import ru.otus.hw.web.exception.SessionExpiredException;

@RequiredArgsConstructor
public class RefreshingOidcLogoutHandler implements LogoutHandler, LogoutSuccessHandler {

  private static final String LATEST =
    RefreshingOidcLogoutHandler.class.getName() + ".authentication";

  private final SessionAccessTokens tokens;

  private final LogoutSuccessHandler delegate;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
    Authentication original) {
    if (original == null || !(original.getPrincipal() instanceof OidcUser)) {
      return;
    }
    try {
      tokens.current(original, request, response);
      Authentication latest = SecurityContextHolder.getContext().getAuthentication();
      if (latest != null) {
        request.setAttribute(LATEST, latest);
      }
    } catch (SessionExpiredException | ProfileUnavailableException ignored) {
      System.out.println("SessionExpiredException | ProfileUnavailableException ignored");
    }
  }

  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
    Authentication original)
    throws IOException, ServletException {
    var latest = (Authentication) request.getAttribute(LATEST);
    request.removeAttribute(LATEST);
    delegate.onLogoutSuccess(request, response, latest == null ? original : latest);
  }
}
