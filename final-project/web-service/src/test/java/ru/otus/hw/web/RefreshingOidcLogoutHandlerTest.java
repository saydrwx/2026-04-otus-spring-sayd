package ru.otus.hw.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import ru.otus.hw.web.security.RefreshingOidcLogoutHandler;
import ru.otus.hw.web.security.SessionAccessTokens;
import ru.otus.hw.web.exception.ProfileUnavailableException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RefreshingOidcLogoutHandlerTest {

  OAuth2AuthenticationToken user(String value) {
    var now = Instant.now();
    var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_PATIENT")),
      new OidcIdToken(value, now, now.plusSeconds(300), Map.of("sub", "patient")), "sub");
    return new OAuth2AuthenticationToken(user, user.getAuthorities(), "hospital");
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void logoutUsesTheLatestIdTokenAfterTheWebSessionIsInvalidated() throws Exception {
    var tokens = mock(SessionAccessTokens.class);
    var old = user("original-id-token");
    var latest = user("latest-id-token");
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    request.getSession();
    when(tokens.current(eq(old), eq(request), eq(response))).thenAnswer(invocation -> {
      SecurityContextHolder.getContext().setAuthentication(latest);
      return "fresh-access";
    });
    var provider = new OidcClientInitiatedLogoutSuccessHandler(
      new InMemoryClientRegistrationRepository(ClientRegistrationFixture.registration()));
    provider.setPostLogoutRedirectUri("http://127.0.0.1:8080/");
    var handler = new RefreshingOidcLogoutHandler(tokens, provider);
    handler.logout(request, response, old);
    request.getSession().invalidate();
    SecurityContextHolder.clearContext();
    handler.onLogoutSuccess(request, response, old);
    assertThat(response.getRedirectedUrl()).contains("id_token_hint=latest-id-token")
      .doesNotContain("original-id-token");
  }

  @Test
  void providerFailureDoesNotStopTheRemainingLocalLogoutHandlers() {
    var tokens = mock(SessionAccessTokens.class);
    when(tokens.current(any(), any(), any())).thenThrow(new ProfileUnavailableException());
    var handler = new RefreshingOidcLogoutHandler(tokens,
      (request, response, authentication) -> response.setStatus(204));
    assertThatCode(() -> handler.logout(new MockHttpServletRequest(), new MockHttpServletResponse(),
      user("original")))
      .doesNotThrowAnyException();
  }
}
