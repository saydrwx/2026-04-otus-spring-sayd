package ru.otus.hw.authorization.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfiguration {

  @Bean
  @Order(1)
  SecurityFilterChain protocolEndpoints(HttpSecurity http) throws Exception {
    OAuth2AuthorizationServerConfigurer server = OAuth2AuthorizationServerConfigurer.authorizationServer();
    http.securityMatcher(server.getEndpointsMatcher())
      .with(server, authorization -> authorization.oidc(Customizer.withDefaults()))
      .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
      .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
        new LoginUrlAuthenticationEntryPoint("/login"),
        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain loginAndApplicationEndpoints(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/actuator/health", "/error").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/registrations").permitAll()
        .requestMatchers(HttpMethod.GET, "/css/auth-login.css").permitAll()
        .anyRequest().authenticated())
      .formLogin(login -> login.loginPage("/login").permitAll());
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings(AuthProperties properties) {
    return AuthorizationServerSettings.builder().issuer(properties.issuer().toString()).build();
  }
}
