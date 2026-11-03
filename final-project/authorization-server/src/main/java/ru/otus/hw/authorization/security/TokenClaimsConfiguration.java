package ru.otus.hw.authorization.security;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import ru.otus.hw.authorization.model.Account;
import ru.otus.hw.authorization.repository.AccountRepository;

@Configuration(proxyBeanMethods = false)
public class TokenClaimsConfiguration {

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(AccountRepository accountRepository) {
    return context -> {
      Account account = accountRepository.findById(UUID.fromString(context.getPrincipal().getName()))
        .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("invalid_grant")));
      context.getClaims().subject(account.id().toString());
      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        context.getClaims().audience(new ArrayList<>(List.of("hospital-api")));
        context.getClaims().claim("roles", new ArrayList<>(List.of(account.role())));
      }
    };
  }
}
