package ru.otus.hw.authorization.security;

import java.time.Duration;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Configuration(proxyBeanMethods = false)
public class OAuthPersistenceConfiguration {

  @Bean
  @DependsOnDatabaseInitialization
  RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbc, PasswordEncoder encoder,
    AuthProperties properties) {
    var repository = new JdbcRegisteredClientRepository(jdbc);
    RegisteredClient client = RegisteredClient.withId("hospital-web")
      .clientId("hospital-web")
      .clientSecret(encoder.encode(properties.clientSecret()))
      .clientName("Hospital web application")
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .redirectUri("http://127.0.0.1:8765/callback")
      .redirectUri("http://127.0.0.1:8080/login/oauth2/code/hospital")
      .postLogoutRedirectUri("http://127.0.0.1:8080/")
      .scope(OidcScopes.OPENID)
      .clientSettings(ClientSettings.builder().requireProofKey(true)
        .requireAuthorizationConsent(false).build())
      .tokenSettings(TokenSettings.builder()
        .authorizationCodeTimeToLive(Duration.ofMinutes(2))
        .accessTokenTimeToLive(Duration.ofMinutes(5))
        .refreshTokenTimeToLive(Duration.ofHours(8))
        .reuseRefreshTokens(false).build())
      .build();
    repository.save(client);
    return repository;
  }

  @Bean
  OAuth2AuthorizationService authorizationService(JdbcTemplate jdbc,
    RegisteredClientRepository clients) {
    return new JdbcOAuth2AuthorizationService(jdbc, clients);
  }

  @Bean
  OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbc,
    RegisteredClientRepository clients) {
    return new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
  }
}
