package ru.otus.hw.web.security;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import ru.otus.hw.web.exception.ProfileUnavailableException;
import ru.otus.hw.web.exception.SessionExpiredException;

@RequiredArgsConstructor
@Component
public class SessionAccessTokensImpl implements SessionAccessTokens {

  private final OAuth2AuthorizedClientManager manager;

  private final OAuth2AuthorizedClientRepository clients;

  private final JwtDecoder webAccessTokenDecoder;

  @Override
  public String current(Authentication principal, HttpServletRequest request,
    HttpServletResponse response) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      expire(principal, request, response);
      throw new SessionExpiredException();
    }
    synchronized (session) {
      try {
        OAuth2AuthorizedClient client = authorize(principal, request, response);
        if (client == null || client.getAccessToken().getExpiresAt() == null
          || !client.getAccessToken().getExpiresAt().isAfter(Instant.now())) {
          throw new SessionExpiredException();
        }
        Jwt token = webAccessTokenDecoder.decode(client.getAccessToken().getTokenValue());
        Object claim = token.getClaims().get("roles");
        if (!(principal.getPrincipal() instanceof OidcUser user) || !user.getSubject()
          .equals(token.getSubject())
          || !(claim instanceof List<?> roles) || roles.size() != 1
          || !(roles.getFirst() instanceof String role)
          || !List.of("PATIENT", "DOCTOR", "ADMIN").contains(role)
          || principal.getAuthorities().stream()
          .noneMatch(a -> a.getAuthority().equals("ROLE_" + role))) {
          throw new SessionExpiredException();
        }
        Object saved = session.getAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (saved instanceof SecurityContext context && context.getAuthentication() != null
          && principal.getName().equals(context.getAuthentication().getName())) {
          SecurityContextHolder.setContext(context);
        }
        return client.getAccessToken().getTokenValue();
      } catch (OAuth2AuthorizationException exception) {
        if (!List.of("invalid_grant", "invalid_token", "invalid_client", "unauthorized_client")
          .contains(exception.getError().getErrorCode())) {
          throw new ProfileUnavailableException();
        }
        expire(principal, request, response);
        throw new SessionExpiredException();
      } catch (SessionExpiredException | JwtException | IllegalArgumentException |
               IllegalStateException exception) {
        expire(principal, request, response);
        throw new SessionExpiredException();
      }
    }
  }

  private OAuth2AuthorizedClient authorize(Authentication principal, HttpServletRequest request,
    HttpServletResponse response) {
    var previous = RequestContextHolder.getRequestAttributes();
    if (previous == null) {
      RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }
    try {
      return manager.authorize(
        OAuth2AuthorizeRequest.withClientRegistrationId("hospital").principal(principal)
          .attribute(HttpServletRequest.class.getName(), request)
          .attribute(HttpServletResponse.class.getName(), response).build());
    } finally {
      if (previous == null) {
        RequestContextHolder.resetRequestAttributes();
      }
    }
  }

  @Override
  public void expire(Authentication principal, HttpServletRequest request,
    HttpServletResponse response) {
    try {
      clients.removeAuthorizedClient("hospital", principal, request, response);
    } catch (IllegalStateException ignored) {
    }
    var session = request.getSession(false);
    if (session != null) {
      try {
        session.invalidate();
      } catch (IllegalStateException ignored) {
      }
    }
    new CookieClearingLogoutHandler("WEBSESSION").logout(request, response, principal);
    SecurityContextHolder.clearContext();
  }
}
