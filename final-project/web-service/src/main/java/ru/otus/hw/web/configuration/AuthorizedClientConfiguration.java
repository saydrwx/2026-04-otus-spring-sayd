package ru.otus.hw.web.configuration;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import ru.otus.hw.web.security.RefreshingOidcTokenResponseClient;

@Configuration(proxyBeanMethods = false)
public class AuthorizedClientConfiguration {

  @Bean
  SecurityContextRepository webSecurityContexts() {
    return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
      new HttpSessionSecurityContextRepository());
  }

  @Bean
  OAuth2AuthorizedClientManager webAuthorizedClientManager(
    ClientRegistrationRepository registrations,
    OAuth2AuthorizedClientRepository clients, WebProperties p,
    JwtDecoderFactory<ClientRegistration> idTokenDecoders, SecurityContextRepository contexts) {
    RestClient http = RestClient.builder().requestFactory(PrivateApiConfiguration.requests(p))
      .messageConverters(c -> {
        c.clear();
        c.add(new FormHttpMessageConverter());
        c.add(new OAuth2AccessTokenResponseHttpMessageConverter());
      }).defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build();
    var rawRefresh = new RestClientRefreshTokenTokenResponseClient();
    rawRefresh.setRestClient(http);
    var refresh = new RefreshingOidcTokenResponseClient(rawRefresh, idTokenDecoders, contexts);
    var manager = new DefaultOAuth2AuthorizedClientManager(registrations, clients);
    manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
      .refreshToken(r -> r.accessTokenResponseClient(refresh).clockSkew(Duration.ofSeconds(30)))
      .build());
    return manager;
  }
}
