package ru.otus.hw.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.otus.hw.web.security.RefreshingOidcTokenResponseClient;

import static org.assertj.core.api.Assertions.*;

class RefreshingOidcTokenResponseClientTest {

  static final String SUBJECT = "c457b22b-b191-4181-873a-4ec6ec58da22";
  MockHttpServletRequest request;
  MockHttpServletResponse response;
  OAuth2RefreshTokenGrantRequest grant;

  @BeforeEach
  void setup() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    request.getSession();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    var now = Instant.now();
    var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_PATIENT")),
      new OidcIdToken("original-id-token", now.minusSeconds(300), now.plusSeconds(60),
        Map.of("sub", SUBJECT, "nonce", "original-nonce")), "sub");
    SecurityContextHolder.getContext()
      .setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), "hospital"));
    grant = new OAuth2RefreshTokenGrantRequest(ClientRegistrationFixture.registration(),
      new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "old-access", now.minusSeconds(300),
        now.minusSeconds(60)),
      new OAuth2RefreshToken("refresh", now.minusSeconds(300)));
  }

  @AfterEach
  void clear() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
  }

  Jwt jwt(String subject, String nonce) {
    return Jwt.withTokenValue("latest-id-token").header("alg", "RS256")
      .issuer("http://127.0.0.1:9000").subject(subject)
      .audience(List.of("hospital-web")).issuedAt(Instant.now())
      .expiresAt(Instant.now().plusSeconds(300)).claim("nonce", nonce).build();
  }

  RefreshingOidcTokenResponseClient client(Jwt id) {
    return new RefreshingOidcTokenResponseClient(
      g -> OAuth2AccessTokenResponse.withToken("fresh-access")
        .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300)
        .refreshToken("rotated-refresh")
        .additionalParameters(Map.of("id_token", "latest-id-token")).build(),
      registration -> raw -> id, new HttpSessionSecurityContextRepository());
  }

  @Test
  void refreshedIdTokenIsSavedForTheNextRequestAndLogout() {
    var result = client(jwt(SUBJECT, "original-nonce")).getTokenResponse(grant);
    assertThat(result.getAccessToken().getTokenValue()).isEqualTo("fresh-access");
    var saved = (SecurityContext) request.getSession()
      .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    var user = (OidcUser) saved.getAuthentication().getPrincipal();
    assertThat(user.getIdToken().getTokenValue()).isEqualTo("latest-id-token");
    assertThat(saved.getAuthentication().getAuthorities()).extracting(a -> a.getAuthority())
      .containsExactly("ROLE_PATIENT");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(
      saved.getAuthentication());
  }

  @Test
  void aDifferentSubjectOrNonceCannotReplaceThePrincipal() {
    for (Jwt id : List.of(jwt("ee13ed22-8bb0-4800-bd96-f6f9df23e586", "original-nonce"),
      jwt(SUBJECT, "different-nonce"))) {
      assertThatThrownBy(() -> client(id).getTokenResponse(grant)).isInstanceOf(
        OAuth2AuthorizationException.class);
    }
    assertThat(((OidcUser) SecurityContextHolder.getContext().getAuthentication()
      .getPrincipal()).getIdToken().getTokenValue()).isEqualTo("original-id-token");
  }

  @Test
  void providerMayOmitTheReplacementIdToken() {
    var client = new RefreshingOidcTokenResponseClient(
      g -> OAuth2AccessTokenResponse.withToken("fresh-access")
        .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300).build(),
      registration -> {
        throw new AssertionError("No ID token to decode");
      }, new HttpSessionSecurityContextRepository());
    client.getTokenResponse(grant);
    assertThat(((OidcUser) SecurityContextHolder.getContext().getAuthentication()
      .getPrincipal()).getIdToken().getTokenValue()).isEqualTo("original-id-token");
  }
}
