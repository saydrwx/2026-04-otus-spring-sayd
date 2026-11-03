package ru.otus.hw.web.security;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RequiredArgsConstructor
public class RefreshingOidcTokenResponseClient implements
  OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> {

  private final OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> delegate;

  private final JwtDecoderFactory<ClientRegistration> idTokenDecoders;

  private final SecurityContextRepository contexts;

  @Override
  public OAuth2AccessTokenResponse getTokenResponse(OAuth2RefreshTokenGrantRequest grant) {
    OAuth2AccessTokenResponse response = delegate.getTokenResponse(grant);
    Object value = response.getAdditionalParameters().get("id_token");
    if (value == null) {
      return response;
    }
    try {
      Authentication current = SecurityContextHolder.getContext().getAuthentication();
      if (!(value instanceof String raw)
        || !(current instanceof OAuth2AuthenticationToken authentication)
        || !(authentication.getPrincipal() instanceof OidcUser oldUser)
        || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
        throw invalid();
      }
      Jwt jwt = idTokenDecoders.createDecoder(grant.getClientRegistration()).decode(raw);
      if (!oldUser.getSubject().equals(jwt.getSubject()) || jwt.getIssuedAt() == null
        || jwt.getExpiresAt() == null
        || (jwt.getClaim("nonce") != null && !Objects.equals(jwt.getClaimAsString("nonce"),
        oldUser.getIdToken().getNonce()))) {
        throw invalid();
      }
      OidcIdToken idToken = new OidcIdToken(raw, jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
      if (idToken.getAuthenticatedAt() != null && !Objects.equals(idToken.getAuthenticatedAt(),
        oldUser.getIdToken().getAuthenticatedAt())) {
        throw invalid();
      }
      var user = new DefaultOidcUser(authentication.getAuthorities(), idToken,
        oldUser.getUserInfo(), "sub");
      var updated = new OAuth2AuthenticationToken(user, authentication.getAuthorities(),
        authentication.getAuthorizedClientRegistrationId());
      updated.setDetails(authentication.getDetails());
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(updated);
      SecurityContextHolder.setContext(context);
      contexts.saveContext(context, attributes.getRequest(), attributes.getResponse());
      return response;
    } catch (JwtException | IllegalArgumentException exception) {
      throw invalid();
    }
  }

  private OAuth2AuthorizationException invalid() {
    return new OAuth2AuthorizationException(
      new OAuth2Error("invalid_token", "Session validation failed", null));
  }
}
