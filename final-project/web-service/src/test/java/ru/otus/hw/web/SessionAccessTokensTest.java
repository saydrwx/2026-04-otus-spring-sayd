package ru.otus.hw.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import ru.otus.hw.web.exception.ProfileUnavailableException;
import ru.otus.hw.web.exception.SessionExpiredException;
import ru.otus.hw.web.security.SessionAccessTokensImpl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionAccessTokensTest {

  static final String SUBJECT = "c457b22b-b191-4181-873a-4ec6ec58da22";
  final HttpSessionOAuth2AuthorizedClientRepository clients = new HttpSessionOAuth2AuthorizedClientRepository();
  final MockHttpServletRequest request = new MockHttpServletRequest();
  final MockHttpServletResponse response = new MockHttpServletResponse();
  final JwtDecoder decoder = mock(JwtDecoder.class);
  OAuth2AuthenticationToken user;
  DefaultOAuth2AuthorizedClientManager manager;
  SessionAccessTokensImpl tokens;

  @BeforeEach
  void setup() {
    var now = Instant.now();
    var registration = ClientRegistrationFixture.registration();
    var oidc = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_PATIENT")),
      new OidcIdToken("id-token", now, now.plusSeconds(300), Map.of("sub", SUBJECT)), "sub");
    user = new OAuth2AuthenticationToken(oidc, oidc.getAuthorities(), "hospital");
    manager = new DefaultOAuth2AuthorizedClientManager(
      new InMemoryClientRegistrationRepository(registration), clients);
    tokens = new SessionAccessTokensImpl(manager, clients, decoder);
    request.setSession(new MockHttpSession());
    clients.saveAuthorizedClient(new OAuth2AuthorizedClient(registration, user.getName(),
      new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "expired-token",
        now.minusSeconds(600), now.minusSeconds(90)),
      new OAuth2RefreshToken("old-refresh", now.minusSeconds(600))), user, request, response);
    when(decoder.decode("fresh-token")).thenReturn(jwt("PATIENT"));
    SecurityContextHolder.getContext().setAuthentication(user);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Jwt jwt(String role) {
    return Jwt.withTokenValue("fresh-token").header("alg", "RS256").subject(SUBJECT)
      .issuer("http://127.0.0.1:9000")
      .audience(List.of("hospital-api")).issuedAt(Instant.now())
      .expiresAt(Instant.now().plusSeconds(300)).claim("roles", List.of(role)).build();
  }

  void refresh(
    org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient<org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest> client) {
    manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
      .refreshToken(r -> r.accessTokenResponseClient(client)).build());
  }

  @Test
  void refreshRotatesAndPersistsTokensInTheSameSession() {
    var calls = new AtomicInteger();
    refresh(grant -> {
      calls.incrementAndGet();
      assertThat(grant.getRefreshToken().getTokenValue()).isEqualTo("old-refresh");
      return OAuth2AccessTokenResponse.withToken("fresh-token")
        .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300)
        .refreshToken("rotated-refresh").build();
    });
    assertThat(tokens.current(user, request, response)).isEqualTo("fresh-token");
    assertThat(tokens.current(user, request, response)).isEqualTo("fresh-token");
    OAuth2AuthorizedClient stored = clients.loadAuthorizedClient("hospital", user, request);
    assertThat(stored.getRefreshToken().getTokenValue()).isEqualTo("rotated-refresh");
    assertThat(calls.get()).isEqualTo(1);
    var second = new MockHttpServletRequest();
    second.setSession(new MockHttpSession());
    assertThat(
      (OAuth2AuthorizedClient) clients.loadAuthorizedClient("hospital", user, second)).isNull();
  }

  @Test
  void invalidGrantClearsTheWebSessionAndAuthentication() {
    refresh(grant -> {
      throw new OAuth2AuthorizationException(new OAuth2Error("invalid_grant"));
    });
    var session = (MockHttpSession) request.getSession();
    assertThatThrownBy(() -> tokens.current(user, request, response)).isInstanceOf(
      SessionExpiredException.class);
    assertThat(session.isInvalid()).isTrue();
    assertThat(response.getCookie("WEBSESSION").getMaxAge()).isZero();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void transientRefreshFailureDoesNotDiscardTheSession() {
    refresh(grant -> {
      throw new OAuth2AuthorizationException(new OAuth2Error("invalid_token_response"));
    });
    var session = (MockHttpSession) request.getSession();
    assertThatThrownBy(() -> tokens.current(user, request, response)).isInstanceOf(
      ProfileUnavailableException.class);
    assertThat(session.isInvalid()).isFalse();
    OAuth2AuthorizedClient stored = clients.loadAuthorizedClient("hospital", user, request);
    assertThat(stored.getRefreshToken().getTokenValue()).isEqualTo("old-refresh");
  }

  @Test
  void changedRoleRequiresFreshLoginBeforeAnyApiCall() {
    refresh(grant -> OAuth2AccessTokenResponse.withToken("fresh-token")
      .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300).refreshToken("rotated-refresh")
      .build());
    when(decoder.decode("fresh-token")).thenReturn(jwt("DOCTOR"));
    var session = (MockHttpSession) request.getSession();
    assertThatThrownBy(() -> tokens.current(user, request, response)).isInstanceOf(
      SessionExpiredException.class);
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void anExpiredTokenWithoutRefreshCannotReachTheApi() {
    var now = Instant.now();
    clients.saveAuthorizedClient(
      new OAuth2AuthorizedClient(ClientRegistrationFixture.registration(), user.getName(),
        new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "expired-token",
          now.minusSeconds(600), now.minusSeconds(90))), user, request, response);
    refresh(grant -> {
      throw new AssertionError("No refresh should be attempted without a refresh token");
    });
    assertThatThrownBy(() -> tokens.current(user, request, response)).isInstanceOf(
      SessionExpiredException.class);
    verifyNoInteractions(decoder);
  }
}
