package ru.otus.hw.clinic.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccount {

  public UUID id() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new AccessDeniedException("An authenticated account is required");
    }
    return UUID.fromString(token.getToken().getSubject());
  }

  public boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getAuthorities().stream()
      .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
  }
}
