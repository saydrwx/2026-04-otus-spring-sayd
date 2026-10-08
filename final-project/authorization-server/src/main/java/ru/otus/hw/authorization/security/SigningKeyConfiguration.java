package ru.otus.hw.authorization.security;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

@Configuration(proxyBeanMethods = false)
public class SigningKeyConfiguration {

  private static final String ALIAS = "hospital-signing";

  @Bean
  JWKSource<SecurityContext> jwkSource(AuthProperties properties, ResourceLoader resources)
    throws Exception {
    KeyStore store = KeyStore.getInstance("PKCS12");
    char[] password = properties.keyStorePassword().toCharArray();
    try (InputStream input = resources.getResource(properties.keyStore()).getInputStream()) {
      store.load(input, password);
    }
    var privateKey = (RSAPrivateKey) store.getKey(ALIAS, password);
    Certificate certificate = store.getCertificate(ALIAS);
    if (privateKey == null || certificate == null) {
      throw new IllegalStateException("Signing key alias hospital-signing is missing");
    }
    var publicKey = (RSAPublicKey) certificate.getPublicKey();
    if (publicKey.getModulus().bitLength() < 2048) {
      throw new IllegalStateException("An RSA signing key of at least 2048 bits is required");
    }
    RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
    key = new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
    return new ImmutableJWKSet<>(new JWKSet(key));
  }

  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> keys) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(keys);
  }
}
