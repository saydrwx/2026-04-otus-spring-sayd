package ru.otus.hw.web;

import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

final class ClientRegistrationFixture {

  private ClientRegistrationFixture() {
  }

  static ClientRegistration registration() {
    return ClientRegistration.withRegistrationId("hospital").clientId("hospital-web")
      .clientSecret("test-client-secret-for-web")
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).scope("openid")
      .redirectUri("http://127.0.0.1:8080/login/oauth2/code/hospital")
      .authorizationUri("http://127.0.0.1:9000/oauth2/authorize")
      .tokenUri("http://127.0.0.1:9000/oauth2/token")
      .jwkSetUri("http://127.0.0.1:9000/oauth2/jwks").issuerUri("http://127.0.0.1:9000")
      .providerConfigurationMetadata(
        Map.of("end_session_endpoint", "http://127.0.0.1:9000/connect/logout")).build();
  }
}
