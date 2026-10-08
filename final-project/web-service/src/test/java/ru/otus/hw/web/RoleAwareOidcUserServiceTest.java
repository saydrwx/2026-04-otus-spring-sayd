package ru.otus.hw.web;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import ru.otus.hw.web.security.RoleAwareOidcUserService;
import ru.otus.hw.web.security.WebAccessTokenValidator;

import static org.assertj.core.api.Assertions.*;

class RoleAwareOidcUserServiceTest {

  static final String SUBJECT = "c457b22b-b191-4181-873a-4ec6ec58da22";
  static final String ISSUER = "http://127.0.0.1:9000";
  KeyPair keys;
  NimbusJwtEncoder encoder;
  RoleAwareOidcUserService users;

  @BeforeEach
  void setup() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    keys = generator.generateKeyPair();
    var key = new RSAKey.Builder((RSAPublicKey) keys.getPublic()).privateKey(keys.getPrivate())
      .keyID("test-key").build();
    encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
    decoder.setJwtValidator(
      new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(ISSUER),
        new WebAccessTokenValidator()));
    users = new RoleAwareOidcUserService(decoder, request -> new DefaultOidcUser(
      List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), request.getIdToken(), "sub"));
  }

  String token(String issuer, String subject, String audience, Object roles, Instant expires) {
    var claims = JwtClaimsSet.builder().issuer(issuer).subject(subject).audience(List.of(audience))
      .issuedAt(expires.isBefore(Instant.now()) ? expires.minusSeconds(300)
        : Instant.now().minusSeconds(10)).expiresAt(expires).claim("roles", roles).build();
    return encoder.encode(
      JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(),
        claims)).getTokenValue();
  }

  OidcUserRequest request(String token) {
    var now = Instant.now();
    var id = new OidcIdToken("id-token-value", now, now.plusSeconds(300),
      Map.of("sub", SUBJECT, "roles", List.of("ADMIN")));
    var access = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token, now,
      now.plusSeconds(300));
    return new OidcUserRequest(ClientRegistrationFixture.registration(), access, id);
  }

  @Test
  void takesRoleOnlyFromValidatedAccessToken() {
    var user = users.loadUser(request(
      token(ISSUER, SUBJECT, "hospital-api", List.of("PATIENT"), Instant.now().plusSeconds(300))));
    assertThat(user.getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_PATIENT")
      .doesNotContain("ROLE_ADMIN");
  }

  @Test
  void rejectsUntrustedIssuerAudienceSubjectAndExpiredTokens() {
    for (String value : List.of(
      token("http://wrong.test", SUBJECT, "hospital-api", List.of("PATIENT"),
        Instant.now().plusSeconds(300)),
      token(ISSUER, SUBJECT, "hospital-web", List.of("PATIENT"), Instant.now().plusSeconds(300)),
      token(ISSUER, "not-a-uuid", "hospital-api", List.of("PATIENT"),
        Instant.now().plusSeconds(300)),
      token(ISSUER, "ee13ed22-8bb0-4800-bd96-f6f9df23e586", "hospital-api", List.of("PATIENT"),
        Instant.now().plusSeconds(300)),
      token(ISSUER, SUBJECT, "hospital-api", List.of("PATIENT"),
        Instant.now().minusSeconds(120)))) {
      assertThatThrownBy(() -> users.loadUser(request(value))).isInstanceOf(
        OAuth2AuthenticationException.class);
    }
  }

  @Test
  void rejectsUnexpectedOrMultipleRoles() {
    for (Object roles : List.of(List.of("OWNER"), List.of("PATIENT", "ADMIN"), List.of(), "PATIENT",
      List.of(123))) {
      String value = token(ISSUER, SUBJECT, "hospital-api", roles, Instant.now().plusSeconds(300));
      assertThatThrownBy(() -> users.loadUser(request(value))).isInstanceOf(
        OAuth2AuthenticationException.class);
    }
  }

  @Test
  void rejectsMissingExpiration() {
    var claims = JwtClaimsSet.builder().issuer(ISSUER).subject(SUBJECT)
      .audience(List.of("hospital-api"))
      .issuedAt(Instant.now()).claim("roles", List.of("PATIENT")).build();
    var value = encoder.encode(
      JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(),
        claims)).getTokenValue();
    assertThatThrownBy(() -> users.loadUser(request(value))).isInstanceOf(
      OAuth2AuthenticationException.class);
  }

  @Test
  void rejectsAnotherSigningKey() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var other = generator.generateKeyPair();
    var key = new RSAKey.Builder((RSAPublicKey) other.getPublic()).privateKey(other.getPrivate())
      .keyID("test-key").build();
    encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    var value = token(ISSUER, SUBJECT, "hospital-api", List.of("PATIENT"),
      Instant.now().plusSeconds(300));
    assertThatThrownBy(() -> users.loadUser(request(value))).isInstanceOf(
      OAuth2AuthenticationException.class);
  }
}
