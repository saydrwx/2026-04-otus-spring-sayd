package ru.otus.hw.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import ru.otus.hw.web.configuration.WebProperties;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain webSecurity(HttpSecurity http, ClientRegistrationRepository registrations,
    OAuth2AuthorizedClientRepository clients, RoleAwareOidcUserService users,
    RestClientAuthorizationCodeTokenResponseClient tokens, WebProperties p,
    SecurityContextRepository contexts, SessionAccessTokens accessTokens) throws Exception {
    var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations,
      "/oauth2/authorization");
    resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    var logout = new OidcClientInitiatedLogoutSuccessHandler(registrations);
    logout.setPostLogoutRedirectUri(p.publicBaseUrl().resolve("/").toString());
    logout.setDefaultTargetUrl("/");
    var refreshingLogout = new RefreshingOidcLogoutHandler(accessTokens, logout);
    var cache = new HttpSessionRequestCache();
    cache.setRequestMatcher(r -> "GET".equals(r.getMethod()) && r.getQueryString() == null
      && ("/account".equals(r.getRequestURI()) || r.getRequestURI().startsWith("/patient/")
      || r.getRequestURI().startsWith("/doctor/") || r.getRequestURI().startsWith("/admin/")));
    http.securityContext(c -> c.securityContextRepository(contexts))
      .requestCache(c -> c.requestCache(cache)).authorizeHttpRequests(a -> a
        .requestMatchers("/", "/doctors", "/doctors/*", "/doctors/*/availability", "/login", "/error",
          "/actuator/health",
          "/css/**", "/js/**", "/webjars/**", "/oauth2/authorization/**", "/login/oauth2/code/**")
        .permitAll()
        .requestMatchers("/register", "/register/complete").anonymous()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/patient/**").hasRole("PATIENT")
        .requestMatchers("/doctor/**").hasRole("DOCTOR")
        .requestMatchers("/account").authenticated().anyRequest().denyAll())
      .oauth2Login(o -> o.loginPage("/login").authorizedClientRepository(clients)
        .authorizationEndpoint(a -> a.authorizationRequestResolver(resolver))
        .tokenEndpoint(t -> t.accessTokenResponseClient(tokens))
        .userInfoEndpoint(u -> u.oidcUserService(users))
        .defaultSuccessUrl("/account", false).failureUrl("/login?error"))
      .logout(l -> l.addLogoutHandler(refreshingLogout).logoutSuccessHandler(refreshingLogout)
        .deleteCookies("WEBSESSION").permitAll())
      .headers(h -> h.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
        .addHeaderWriter((request, response) -> {
          if (!response.containsHeader("Content-Security-Policy")) {
            response.setHeader("Content-Security-Policy",
              "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; "
                + "form-action 'self' " + p.issuer()
                + "; frame-ancestors 'none'; base-uri 'none'; object-src 'none'");
          }
        }));
    return http.build();
  }
}
