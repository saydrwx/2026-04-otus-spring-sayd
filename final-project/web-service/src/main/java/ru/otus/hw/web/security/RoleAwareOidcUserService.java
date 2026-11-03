package ru.otus.hw.web.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class RoleAwareOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private static final Set<String> ROLES = Set.of("PATIENT", "DOCTOR", "ADMIN");

  private final JwtDecoder decoder;

  private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

  public RoleAwareOidcUserService(JwtDecoder decoder,
    OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
    this.decoder = decoder;
    this.delegate = delegate;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest request) {
    OidcUser user = delegate.loadUser(request);
    try {
      Jwt token = decoder.decode(request.getAccessToken().getTokenValue());
      Object claim = token.getClaims().get("roles");
      if (!token.getSubject().equals(user.getSubject()) || !(claim instanceof List<?> roles)
        || roles.size() != 1 || !(roles.getFirst() instanceof String role) || !ROLES.contains(
        role)) {
        throw invalid();
      }
      var authorities = new HashSet<GrantedAuthority>(user.getAuthorities());
      authorities.removeIf(a -> a.getAuthority().startsWith("ROLE_"));
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), "sub");
    } catch (JwtException | IllegalArgumentException exception) {
      throw invalid();
    }
  }

  private OAuth2AuthenticationException invalid() {
    return new OAuth2AuthenticationException(
      new OAuth2Error("invalid_token", "Account validation failed", null));
  }
}
