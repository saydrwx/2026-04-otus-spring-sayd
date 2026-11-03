package ru.otus.hw.web.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import ru.otus.hw.web.security.RoleAwareOidcUserService;
import ru.otus.hw.web.security.WebAccessTokenValidator;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebProperties.class)
public class OAuthClientConfiguration {

  @Bean
  ClientRegistrationRepository clientRegistrations(WebProperties p) {
    ClientRegistration registration = ClientRegistration.withRegistrationId("hospital")
      .clientId("hospital-web").clientSecret(p.clientSecret())
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .redirectUri(p.publicBaseUrl().resolve("/login/oauth2/code/hospital").toString())
      .scope("openid").authorizationUri(p.issuer().resolve("/oauth2/authorize").toString())
      .tokenUri(p.authBackendBaseUrl().resolve("/oauth2/token").toString())
      .jwkSetUri(p.authBackendBaseUrl().resolve("/oauth2/jwks").toString())
      .userInfoUri(p.authBackendBaseUrl().resolve("/userinfo").toString())
      .userNameAttributeName("sub")
      .issuerUri(p.issuer().toString())
      .providerConfigurationMetadata(
        Map.of("end_session_endpoint", p.issuer().resolve("/connect/logout").toString()))
      .clientName("Hospital").build();
    return new InMemoryClientRegistrationRepository(registration);
  }

  @Bean
  OAuth2AuthorizedClientRepository authorizedClients() {
    return new HttpSessionOAuth2AuthorizedClientRepository();
  }

  @Bean
  RestClientAuthorizationCodeTokenResponseClient authorizationCodeClient(WebProperties p) {
    RestClient http = RestClient.builder().requestFactory(HttpClients.requests(p))
      .messageConverters(converters -> {
        converters.clear();
        converters.add(new FormHttpMessageConverter());
        converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
      }).defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build();
    var client = new RestClientAuthorizationCodeTokenResponseClient();
    client.setRestClient(http);
    return client;
  }

  @Bean
  JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory(WebProperties p) {
    var cache = new ConcurrentHashMap<String, JwtDecoder>();
    return registration -> cache.computeIfAbsent(registration.getRegistrationId(), ignored -> {
      var decoder = NimbusJwtDecoder.withJwkSetUri(registration.getProviderDetails().getJwkSetUri())
        .restOperations(new RestTemplate(HttpClients.requests(p))).build();
      decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefault(), new OidcIdTokenValidator(registration)));
      return decoder;
    });
  }

  @Bean
  JwtDecoder webAccessTokenDecoder(WebProperties p) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
        p.authBackendBaseUrl().resolve("/oauth2/jwks").toString())
      .restOperations(new RestTemplate(HttpClients.requests(p))).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
      JwtValidators.createDefaultWithIssuer(p.issuer().toString()),
      new WebAccessTokenValidator()));
    return decoder;
  }

  @Bean
  RoleAwareOidcUserService roleAwareOidcUsers(JwtDecoder webAccessTokenDecoder, WebProperties p) {
    RestTemplate http = new RestTemplate(HttpClients.requests(p));
    http.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
    var userInfo = new DefaultOAuth2UserService();
    userInfo.setRestOperations(http);
    var oidc = new OidcUserService();
    oidc.setOauth2UserService(userInfo);
    return new RoleAwareOidcUserService(webAccessTokenDecoder, oidc);
  }
}
