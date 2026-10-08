package ru.otus.hw.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.otus.hw.authorization.dto.RegistrationInputDto;
import ru.otus.hw.authorization.repository.AccountRepository;
import ru.otus.hw.authorization.service.RegistrationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Full application, real PostgreSQL, production security chains and test-generated PKCS12.
@SpringBootTest(classes = Application.class, properties = "spring.jackson.deserialization.fail-on-unknown-properties=true")
@AutoConfigureMockMvc
@Testcontainers
class RegistrationIntegrationTest {

  private static final String PASSWORD = "A long test passphrase 2026";
  private static final String CLIENT_SECRET = "test-only-client-secret-2026";
  private static final String KEY_PASSWORD = "test-only-keystore-password";
  private static final Path KEYSTORE = createKeyStore();

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.auth.key-store", () -> KEYSTORE.toUri().toString());
    registry.add("app.auth.key-store-password", () -> KEY_PASSWORD);
    registry.add("app.auth.client-secret", () -> CLIENT_SECRET);
    registry.add("app.auth.issuer", () -> "http://127.0.0.1:9000");
  }

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper json;
  @Autowired
  AccountRepository accounts;
  @Autowired
  RegistrationService registration;
  @Autowired
  PasswordEncoder encoder;
  @Autowired
  JwtDecoder decoder;
  @Autowired
  JdbcTemplate jdbc;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM oauth2_authorization_consent");
    jdbc.update("DELETE FROM oauth2_authorization");
    jdbc.update(
      "DELETE FROM accounts WHERE lower(btrim(email) COLLATE \"C\") LIKE '%@example.test'");
  }

  @AfterAll
  static void removeKeys() throws Exception {
    FileSystemUtils.deleteRecursively(KEYSTORE.getParent());
  }

  @Test
  void registersAnonymousPatientWithHashAndNoAutomaticAuthentication() throws Exception {
    var csrf = csrfSession();
    var result = signup(csrf, "  Alice@Example.Test  ", PASSWORD).andExpect(status().isCreated())
      .andExpect(jsonPath("$.email").value("alice@example.test"))
      .andExpect(jsonPath("$.role").value("PATIENT"))
      .andExpect(jsonPath("$.password").doesNotExist())
      .andExpect(jsonPath("$.access_token").doesNotExist()).andReturn();
    UUID id = UUID.fromString(
      json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    var account = accounts.findById(id).orElseThrow();
    assertThat(account.password()).startsWith("{bcrypt}").isNotEqualTo(PASSWORD);
    assertThat(encoder.matches(PASSWORD, account.password())).isTrue();
    assertThat(csrf.session()
      .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
  }

  @Test
  void csrfIsRequiredAndTokenMustBelongToItsSession() throws Exception {
    mvc.perform(post("/api/registrations").contentType(MediaType.APPLICATION_JSON)
      .content(body("csrf@example.test", PASSWORD))).andExpect(status().isForbidden());
    var first = csrfSession();
    var second = csrfSession();
    mvc.perform(
        post("/api/registrations").session(second.session()).header(first.header(), first.token())
          .contentType(MediaType.APPLICATION_JSON).content(body("csrf@example.test", PASSWORD)))
      .andExpect(status().isForbidden());
    signup(first, "csrf@example.test", PASSWORD).andExpect(status().isCreated());
    mvc.perform(post("/login").param("username", "csrf@example.test").param("password", PASSWORD))
      .andExpect(status().isForbidden());
  }

  @Test
  void rejectsUnknownRoleFieldsAndInvalidCredentialsWithoutEchoingThem() throws Exception {
    var csrf = csrfSession();
    for (String payload : List.of("{}", body("invalid-email", PASSWORD),
      body("invalid@example.test", "too-short"),
      body("invalid@example.test", " ".repeat(20)), body("invalid@example.test", "€".repeat(25)),
      body("invalid@example.test", PASSWORD).replace("{", "{\"role\":\"ADMIN\","))) {
      mvc.perform(
          post("/api/registrations").session(csrf.session()).header(csrf.header(), csrf.token())
            .contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(
          org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))))
        .andExpect(content().string(
          org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("too-short"))));
    }
    assertThat(accounts.emailExists("invalid@example.test")).isFalse();
  }

  @Test
  void rejectsDuplicateAndSeededEmailsCaseInsensitively() throws Exception {
    signup(csrfSession(), "alice@example.test", PASSWORD).andExpect(status().isCreated());
    signup(csrfSession(), "ALICE@EXAMPLE.TEST", PASSWORD).andExpect(status().isConflict());
    signup(csrfSession(), " ADMIN@EXAMPLE.COM ", PASSWORD).andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email='alice@example.test'",
      Long.class)).isEqualTo(1L);
  }

  @Test
  @Timeout(45)
  void concurrentEquivalentEmailSignupsHaveOnlyOneWinner() throws Exception {
    var first = csrfSession();
    var second = csrfSession();
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var tasks = new ArrayList<java.util.concurrent.Future<Integer>>();
      for (var csrf : List.of(first, second)) {
        tasks.add(executor.submit(() -> {
          barrier.await(5, TimeUnit.SECONDS);
          return signup(csrf, csrf == first ? "Race@Example.Test" : "race@example.test",
            PASSWORD).andReturn().getResponse().getStatus();
        }));
      }
      var statuses = new ArrayList<Integer>();
      for (var task : tasks) {
        statuses.add(task.get(20, TimeUnit.SECONDS));
      }
      assertThat(statuses).containsExactlyInAnyOrder(201, 409);
      assertThat(
        jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email='race@example.test'",
          Long.class)).isEqualTo(1L);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void normalizedIndexProtectsRepositoryWritesAndLegacyMixedCaseLogin() throws Exception {
    String hash = encoder.encode(PASSWORD);
    jdbc.update("INSERT INTO accounts(id,email,password,role) VALUES (?,? ,?,'PATIENT')",
      UUID.randomUUID(), "Legacy@Example.Test", hash);
    signup(csrfSession(), "legacy@example.test", PASSWORD).andExpect(status().isConflict());
    assertThat(accounts.insertPatient(UUID.randomUUID(), "legacy@example.test", hash)).isFalse();
    login(" LEGACY@EXAMPLE.TEST ", PASSWORD);
  }

  @Test
  void registeredAccountUsesRealAuthorizationCodePkceAndUuidSubject() throws Exception {
    var created = signup(csrfSession(), "oauth@example.test", PASSWORD).andExpect(
      status().isCreated()).andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
    MockHttpSession session = login("OAUTH@EXAMPLE.TEST", PASSWORD);
    String verifier = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-._~";
    String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
      MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    String redirect = "http://127.0.0.1:8765/callback";
    var authorization = mvc.perform(get("/oauth2/authorize")
        .session(session)
        .queryParam("response_type", "code")
        .queryParam("client_id", "hospital-web")
        .queryParam("redirect_uri", redirect)
        .queryParam("scope", "openid")
        .queryParam("state", "test-state")
        .queryParam("code_challenge", challenge)
        .queryParam("code_challenge_method", "S256"))
      .andExpect(status().is3xxRedirection())
      .andReturn()
      .getResponse();
    var callback = UriComponentsBuilder.fromUriString(authorization.getHeader(HttpHeaders.LOCATION))
      .build().getQueryParams();
    assertThat(callback.getFirst("state")).isEqualTo("test-state");
    assertThat(callback.getFirst("code")).isNotBlank();
    String code = UriUtils.decode(callback.getFirst("code"), StandardCharsets.UTF_8);
    var exchange = mvc.perform(post("/oauth2/token").with(httpBasic("hospital-web", CLIENT_SECRET))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("grant_type", "authorization_code")
        .param("redirect_uri", redirect).param("code", code).param("code_verifier", verifier))
      .andExpect(status().isOk()).andReturn().getResponse();
    var tokens = json.readTree(exchange.getContentAsString());
    String access = tokens.get("access_token").asText();
    var jwt = decoder.decode(access);
    assertThat(jwt.getSubject()).isEqualTo(id);
    assertThat(jwt.getAudience()).contains("hospital-api");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("PATIENT");
    mvc.perform(get("/userinfo").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
      .andExpect(status().isOk()).andExpect(jsonPath("$.sub").value(id));
  }

  @Test
  void bcryptDoesNotStripOrTruncateTheAcceptedPassword() throws Exception {
    String pass = "  meaningful spaces  ";
    signup(csrfSession(), "spaces@example.test", pass).andExpect(status().isCreated());
    var account = accounts.findByEmail("spaces@example.test").orElseThrow();
    assertThat(encoder.matches(pass, account.password())).isTrue();
    assertThat(encoder.matches(pass.strip(), account.password())).isFalse();
    signup(csrfSession(), "unicode@example.test", "€".repeat(24)).andExpect(status().isCreated());
    assertThat(encoder.matches("€".repeat(24),
      accounts.findByEmail("unicode@example.test").orElseThrow().password())).isTrue();
  }

  private MockHttpSession login(String email, String password) throws Exception {
    var result = mvc.perform(
        post("/login").with(csrf()).param("username", email).param("password", password))
      .andExpect(status().is3xxRedirection()).andReturn();
    var session = (MockHttpSession) result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    var context = (SecurityContext) session.getAttribute(
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    assertThat(context).isNotNull();
    assertThat(context.getAuthentication().isAuthenticated()).isTrue();
    assertThat(context.getAuthentication().getName()).isEqualTo(
      accounts.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT)).orElseThrow().id()
        .toString());
    return session;
  }

  private CsrfSession csrfSession() throws Exception {
    var result = mvc.perform(get("/api/csrf")).andExpect(status().isOk())
      .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
      .andReturn();
    var token = json.readTree(result.getResponse().getContentAsString());
    return new CsrfSession((MockHttpSession) result.getRequest().getSession(false),
      token.get("headerName").asText(), token.get("token").asText());
  }

  private ResultActions signup(CsrfSession csrf, String email, String password) throws Exception {
    return mvc.perform(
      post("/api/registrations").session(csrf.session()).header(csrf.header(), csrf.token())
        .contentType(MediaType.APPLICATION_JSON).content(body(email, password)));
  }

  private String body(String email, String password) throws Exception {
    return json.writeValueAsString(Map.of("email", email, "password", password));
  }

  private record CsrfSession(MockHttpSession session, String header, String token) {

  }

  private static Path createKeyStore() {
    try {
      Path directory = Files.createTempDirectory("registration-signing-");
      Path keystore = directory.resolve("signing.p12");
      String executable =
        System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).startsWith("windows")
          ? "keytool.exe" : "keytool";
      Path keytool = Path.of(System.getProperty("java.home"), "bin", executable);
      var process = new ProcessBuilder(keytool.toString(), "-genkeypair", "-noprompt", "-alias",
        "hospital-signing",
        "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12", "-keystore",
        keystore.toString(),
        "-storepass", KEY_PASSWORD, "-keypass", KEY_PASSWORD, "-dname", "CN=Registration test",
        "-validity", "2")
        .redirectErrorStream(true).redirectOutput(directory.resolve("keytool.log").toFile())
        .start();
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IllegalStateException("Test key generation timed out");
      }
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
          "Test key generation failed; use a complete JDK with keytool");
      }
      return keystore;
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create test signing keystore", exception);
    }
  }
}
