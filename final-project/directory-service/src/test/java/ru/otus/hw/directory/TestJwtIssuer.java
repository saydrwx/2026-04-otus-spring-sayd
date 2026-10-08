package ru.otus.hw.directory;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

// Serves only a temporary public JWK. The application's real JwtDecoder is not mocked/replaced.
final class TestJwtIssuer implements AutoCloseable {

  static final String ISSUER = "https://directory-suite.example.test";
  private final RSAKey key;
  private final NimbusJwtEncoder encoder;
  private HttpServer server;

  TestJwtIssuer() {
    try {
      key = new RSAKeyGenerator(2048).keyID("directory-test-key").generate();
      encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    } catch (com.nimbusds.jose.JOSEException exception) {
      throw new IllegalStateException("Cannot create test signing key", exception);
    }
  }

  synchronized String jwkSetUri() {
    if (server == null) {
      try {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        server.createContext("/jwks", exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (var response = exchange.getResponseBody()) {
            response.write(body);
          } finally {
            exchange.close();
          }
        });
        server.start();
      } catch (java.io.IOException exception) {
        throw new IllegalStateException("Cannot start test JWK server", exception);
      }
    }
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
  }

  String bearer(String role) {
    return bearer(claims -> claims.claim("roles", List.of(role)));
  }

  String bearer(Consumer<JwtClaimsSet.Builder> customize) {
    Instant now = Instant.now();
    var claims = JwtClaimsSet.builder().issuer(ISSUER)
      .subject("00000000-0000-0000-0000-000000000001")
      .audience(List.of("hospital-api")).issuedAt(now).notBefore(now.minusSeconds(1))
      .expiresAt(now.plusSeconds(300)).claim("roles", List.of("ADMIN"));
    customize.accept(claims);
    var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build();
    return "Bearer " + encoder.encode(JwtEncoderParameters.from(header, claims.build()))
      .getTokenValue();
  }

  @Override
  public synchronized void close() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
  }
}
