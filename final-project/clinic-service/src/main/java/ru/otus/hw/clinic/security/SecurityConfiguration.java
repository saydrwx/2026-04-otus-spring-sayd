package ru.otus.hw.clinic.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain clinicSecurity(HttpSecurity http) throws Exception {
    http
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(this::configureAuthorization)
      .oauth2ResourceServer(resource ->
        resource.jwt(jwt ->
          jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return converter;
  }

  private void configureAuthorization(
    AuthorizeHttpRequestsConfigurer<HttpSecurity>
      .AuthorizationManagerRequestMatcherRegistry authorize
  ) {
    configurePublicEndpoints(authorize);
    configureDocumentEndpoints(authorize);
    configureClinicalEndpoints(authorize);
    configureAppointmentEndpoints(authorize);
    configureProfileEndpoints(authorize);

    authorize.anyRequest().denyAll();
  }

  private void configurePublicEndpoints(
    AuthorizeHttpRequestsConfigurer<HttpSecurity>
      .AuthorizationManagerRequestMatcherRegistry authorize
  ) {
    authorize
      .requestMatchers("/actuator/health", "/error")
      .permitAll()
      .requestMatchers(
        HttpMethod.GET,
        "/api/doctors/*/schedule",
        "/api/doctors/*/availability")
      .permitAll();
  }

  private void configureDocumentEndpoints(
    AuthorizeHttpRequestsConfigurer<HttpSecurity>
      .AuthorizationManagerRequestMatcherRegistry authorize
  ) {
    authorize
      .requestMatchers(HttpMethod.POST, "/api/documents")
      .hasRole("PATIENT")
      .requestMatchers(HttpMethod.DELETE, "/api/documents/*")
      .hasRole("PATIENT")
      .requestMatchers(
        HttpMethod.GET,
        "/api/documents/*",
        "/api/documents/*/content",
        "/api/patients/*/documents")
      .hasAnyRole("PATIENT", "DOCTOR");
  }

  private void configureClinicalEndpoints(
    AuthorizeHttpRequestsConfigurer<HttpSecurity>
      .AuthorizationManagerRequestMatcherRegistry authorize
  ) {
    authorize
      .requestMatchers(
        HttpMethod.POST,
        "/api/appointments/*/report",
        "/api/appointments/*/no-show")
      .hasRole("DOCTOR")
      .requestMatchers(
        HttpMethod.GET,
        "/api/appointments/*/report",
        "/api/patients/*/reports",
        "/api/appointments/*/review")
      .hasAnyRole("PATIENT", "DOCTOR")
      .requestMatchers(HttpMethod.POST, "/api/appointments/*/review")
      .hasRole("PATIENT")
      .requestMatchers(HttpMethod.PUT, "/api/appointments/*/review")
      .hasRole("PATIENT")
      .requestMatchers(HttpMethod.DELETE, "/api/appointments/*/review")
      .hasRole("PATIENT");
  }

  private void configureAppointmentEndpoints(
    AuthorizeHttpRequestsConfigurer<HttpSecurity>
      .AuthorizationManagerRequestMatcherRegistry authorize
  ) {
    authorize
      .requestMatchers(HttpMethod.POST, "/api/appointments")
      .hasRole("PATIENT")
      .requestMatchers(HttpMethod.POST, "/api/appointments/*/cancel")
      .hasAnyRole("PATIENT", "ADMIN")
      .requestMatchers(
        HttpMethod.GET,
        "/api/appointments",
        "/api/appointments/*")
      .hasAnyRole("PATIENT", "DOCTOR", "ADMIN");
  }

  private void configureProfileEndpoints(
    AuthorizeHttpRequestsConfigurer<HttpSecurity>
      .AuthorizationManagerRequestMatcherRegistry authorize
  ) {
    authorize
      .requestMatchers("/api/patients/me")
      .hasRole("PATIENT")
      .requestMatchers("/api/doctors/me")
      .hasRole("DOCTOR");
  }

  @Bean
  JwtDecoder clinicJwtDecoder(
    @Value("${AUTH_ISSUER:http://127.0.0.1:9000}") String issuer,
    @Value("${AUTH_JWK_SET_URI:http://127.0.0.1:9000/oauth2/jwks}") String jwkSetUri
  ) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
      JwtValidators.createDefaultWithIssuer(issuer), new AccountTokenValidator()));
    return decoder;
  }
}
