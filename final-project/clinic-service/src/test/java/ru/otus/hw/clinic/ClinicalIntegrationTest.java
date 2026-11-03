package ru.otus.hw.clinic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.otus.hw.clinic.client.DirectoryClient;
import ru.otus.hw.clinic.client.DirectoryDoctorDto;
import ru.otus.hw.clinic.dto.ReportInputDto;
import ru.otus.hw.clinic.exception.DirectoryUnavailableException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.AppointmentStatus;
import ru.otus.hw.clinic.model.MedicalReport;
import ru.otus.hw.clinic.repository.AppointmentRepository;
import ru.otus.hw.clinic.repository.MedicalReportRepository;
import ru.otus.hw.clinic.repository.PatientRepository;
import ru.otus.hw.clinic.repository.ReviewRepository;
import ru.otus.hw.clinic.security.AccountTokenValidator;
import ru.otus.hw.clinic.service.VisitService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Separate context/database from the prior milestone's regression suite.
@SpringBootTest(properties = "spring.jackson.deserialization.fail-on-unknown-properties=true")
@AutoConfigureMockMvc
@Testcontainers
@Import(ClinicalIntegrationTest.Configuration.class)
class ClinicalIntegrationTest {

  private static final String ISSUER = "http://127.0.0.1:9000";
  private static final String PATIENT = "00000000-0000-0000-0000-000000000004";
  private static final String OTHER = "10000000-0000-0000-0000-000000000001";
  private static final String DOCTOR1 = "00000000-0000-0000-0000-000000000002";
  private static final String DOCTOR2 = "00000000-0000-0000-0000-000000000003";
  private static final String DOCTOR3 = "10000000-0000-0000-0000-000000000002";
  private static final String ADMIN = "00000000-0000-0000-0000-000000000001";
  private static final Instant BOOKING_TIME = Instant.parse("2026-11-02T06:00:00Z");
  private static final Instant VISIT_END = Instant.parse("2026-11-04T06:30:00Z");
  private static final String REPORT = """
    {"findings":"Test findings","diagnosis":"Test diagnosis","recommendations":"Test recommendations"}
    """;
  private static final String REVIEW = "{\"rating\":5,\"text\":\"Thank you\"}";
  private static final RSAKey KEY = key();
  private static final AtomicBoolean OFFLINE = new AtomicBoolean();
  private static final AtomicLong DOCTOR1_HOSPITAL = new AtomicLong(2);

  private static final java.nio.file.Path DOCUMENT_ROOT = temporaryDocumentRoot();

  private static java.nio.file.Path temporaryDocumentRoot() {
    try {
      return java.nio.file.Files.createTempDirectory("clinic-regression-documents-");
    } catch (java.io.IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @org.junit.jupiter.api.AfterAll
  static void removeDocumentRoot() throws java.io.IOException {
    org.springframework.util.FileSystemUtils.deleteRecursively(DOCUMENT_ROOT);
  }

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("app.documents.root", () -> DOCUMENT_ROOT.toString());
    properties.add("app.documents.cleanup-enabled", () -> "false");
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper json;
  @Autowired
  Clock clock;
  @Autowired
  DataSource dataSource;
  @Autowired
  PatientRepository patients;
  @Autowired
  AppointmentRepository appointments;
  @Autowired
  MedicalReportRepository reports;
  @Autowired
  ReviewRepository reviews;
  @Autowired
  VisitService visitService;

  @BeforeEach
  void reset() {
    reviews.deleteAllInBatch();
    reports.deleteAllInBatch();
    appointments.deleteAllInBatch();
    patients.deleteAll();
    new JdbcTemplate(dataSource).update("DELETE FROM doctor_accounts WHERE account_id = ?",
      UUID.fromString(DOCTOR3));
    time(BOOKING_TIME);
    OFFLINE.set(false);
    DOCTOR1_HOSPITAL.set(2);
  }

  @Test
  void reportCompletesVisitAtEndAndIsImmutableThroughTheApi() throws Exception {
    UUID id = book(PATIENT, 1);
    time(VISIT_END.minusNanos(1));
    report(id, DOCTOR1).andExpect(status().isConflict());
    assertThat(reports.count()).isZero();
    time(VISIT_END);
    report(id, DOCTOR1).andExpect(status().isCreated())
      .andExpect(header().string("Location", "/api/appointments/" + id + "/report"));
    var stored = appointments.findById(id).orElseThrow();
    assertThat(stored.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    assertThat(stored.getOutcomeAt()).isEqualTo(VISIT_END);
    assertThat(stored.getOutcomeBy()).isEqualTo(UUID.fromString(DOCTOR1));
    mvc.perform(get(path(id, "report")).header("Authorization", bearer(PATIENT, "PATIENT")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.diagnosis").value("Test diagnosis"));
    mvc.perform(get(path(id, "report")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isOk());
    report(id, DOCTOR1).andExpect(status().isConflict());
    mvc.perform(put(path(id, "report")).header("Authorization", bearer(DOCTOR1, "DOCTOR"))
      .contentType(MediaType.APPLICATION_JSON).content(REPORT)).andExpect(status().isForbidden());
    assertThat(reports.count()).isEqualTo(1);
  }

  @Test
  void noShowRequiresAssignedDoctorAndAnEndedBookedVisit() throws Exception {
    UUID id = book(PATIENT, 1);
    mvc.perform(post(path(id, "no-show")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isConflict());
    time(VISIT_END);
    report(id, DOCTOR2).andExpect(status().isNotFound());
    for (String role : List.of("PATIENT", "ADMIN")) {
      mvc.perform(post(path(id, "report")).header("Authorization", bearer(ADMIN, role))
        .contentType(MediaType.APPLICATION_JSON).content(REPORT)).andExpect(status().isForbidden());
    }
    mvc.perform(post(path(id, "no-show")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isNoContent());
    assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(
      AppointmentStatus.NO_SHOW);
    assertThat(reports.count()).isZero();
    report(id, DOCTOR1).andExpect(status().isConflict());
    mvc.perform(post(path(id, "no-show")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isConflict());
    mvc.perform(post(path(id, "review")).header("Authorization", bearer(PATIENT, "PATIENT"))
      .contentType(MediaType.APPLICATION_JSON).content(REVIEW)).andExpect(status().isConflict());
  }

  @Test
  void medicalContentIsHiddenFromOtherPatientsOtherHospitalsAndAdmin() throws Exception {
    UUID id = book(PATIENT, 1);
    time(VISIT_END);
    report(id, DOCTOR1).andExpect(status().isCreated());
    UUID patientId = patients.findByAccountId(UUID.fromString(PATIENT)).orElseThrow().getId();
    for (String account : List.of(OTHER, DOCTOR2, ADMIN)) {
      String role = account.equals(OTHER) ? "PATIENT" : account.equals(ADMIN) ? "ADMIN" : "DOCTOR";
      int expected = role.equals("ADMIN") ? 403 : 404;
      mvc.perform(get(path(id, "report")).header("Authorization", bearer(account, role)))
        .andExpect(status().is(expected));
      mvc.perform(get("/api/patients/" + patientId + "/reports").header("Authorization",
          bearer(account, role)))
        .andExpect(status().is(expected));
    }
    mvc.perform(get("/api/patients/" + patientId + "/reports").header("Authorization",
        bearer(PATIENT, "PATIENT")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get("/api/patients/" + patientId + "/reports").param("size", "101")
      .header("Authorization", bearer(PATIENT, "PATIENT"))).andExpect(status().isBadRequest());
    mvc.perform(get(path(id, "report"))).andExpect(status().isUnauthorized());
  }

  @Test
  void reviewsAreOwnerManagedOnePerCompletedAppointment() throws Exception {
    UUID id = book(PATIENT, 1);
    review(id, "post", PATIENT, "PATIENT", REVIEW).andExpect(status().isConflict());
    time(VISIT_END);
    report(id, DOCTOR1).andExpect(status().isCreated());
    var created = review(id, "post", PATIENT, "PATIENT", REVIEW).andExpect(status().isCreated())
      .andReturn();
    String createdAt = json.readTree(created.getResponse().getContentAsString()).get("createdAt")
      .asText();
    review(id, "post", PATIENT, "PATIENT", REVIEW).andExpect(status().isConflict());
    mvc.perform(get(path(id, "review")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isOk());
    review(id, "put", OTHER, "PATIENT", REVIEW).andExpect(status().isNotFound());
    review(id, "put", DOCTOR1, "DOCTOR", REVIEW).andExpect(status().isForbidden());
    mvc.perform(get(path(id, "review")).header("Authorization", bearer(DOCTOR2, "DOCTOR")))
      .andExpect(status().isNotFound());
    mvc.perform(get(path(id, "review")).header("Authorization", bearer(ADMIN, "ADMIN")))
      .andExpect(status().isForbidden());
    time(VISIT_END.plusSeconds(60));
    review(id, "put", PATIENT, "PATIENT", "{\"rating\":4,\"text\":\" \"}")
      .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(4))
      .andExpect(jsonPath("$.text").isEmpty()).andExpect(jsonPath("$.createdAt").value(createdAt));
    mvc.perform(delete(path(id, "review")).header("Authorization", bearer(PATIENT, "PATIENT")))
      .andExpect(status().isNoContent());
    mvc.perform(get(path(id, "review")).header("Authorization", bearer(PATIENT, "PATIENT")))
      .andExpect(status().isNotFound());
    review(id, "post", PATIENT, "PATIENT", REVIEW).andExpect(status().isCreated());
    assertThat(reviews.count()).isEqualTo(1);
  }

  @Test
  void invalidClinicalInputCannotChangeAppointmentState() throws Exception {
    UUID id = book(PATIENT, 1);
    time(VISIT_END);
    for (String body : List.of("{}", REPORT.replace("Test findings", " "),
      REPORT.replace("Test diagnosis", "x".repeat(10001)),
      REPORT.replace("{", "{\"patientId\":\"" + UUID.randomUUID() + "\","))) {
      mvc.perform(post(path(id, "report")).header("Authorization", bearer(DOCTOR1, "DOCTOR"))
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(
      AppointmentStatus.BOOKED);
    report(id, DOCTOR1).andExpect(status().isCreated());
    for (String body : List.of("{}", "{\"rating\":0}", "{\"rating\":6}",
      json.writeValueAsString(java.util.Map.of("rating", 5, "text", "x".repeat(2001))))) {
      review(id, "post", PATIENT, "PATIENT", body).andExpect(status().isBadRequest());
    }
    assertThat(reviews.count()).isZero();
  }

  @Test
  void failedReportInsertRollsBackAppointmentCompletion() throws Exception {
    UUID id = book(PATIENT, 1);
    // Deliberately inconsistent fixture forces a unique-report failure after the state mutation.
    reports.saveAndFlush(new MedicalReport(appointments.findById(id).orElseThrow(),
      "Existing", "Existing", "Existing", BOOKING_TIME, UUID.fromString(DOCTOR1)));
    time(VISIT_END);
    report(id, DOCTOR1).andExpect(status().isConflict());
    var appointment = appointments.findById(id).orElseThrow();
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    assertThat(appointment.getOutcomeAt()).isNull();
    assertThat(reports.count()).isEqualTo(1);
  }

  @Test
  @Timeout(30)
  void competingReportAndNoShowHaveOneOutcomeWinner() throws Exception {
    UUID id = book(PATIENT, 1);
    time(VISIT_END);
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var reportStatus = executor.submit(() -> {
        barrier.await(5, TimeUnit.SECONDS);
        return report(id, DOCTOR1).andReturn().getResponse().getStatus();
      });
      var noShowStatus = executor.submit(() -> {
        barrier.await(5, TimeUnit.SECONDS);
        return mvc.perform(
            post(path(id, "no-show")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
          .andReturn().getResponse().getStatus();
      });
      int first = reportStatus.get(15, TimeUnit.SECONDS);
      int second = noShowStatus.get(15, TimeUnit.SECONDS);
      assertThat(first == 201 && second == 409 || first == 409 && second == 204).isTrue();
      var outcome = appointments.findById(id).orElseThrow().getStatus();
      assertThat(outcome).isEqualTo(
        first == 201 ? AppointmentStatus.COMPLETED : AppointmentStatus.NO_SHOW);
      assertThat(reports.count()).isEqualTo(first == 201 ? 1 : 0);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  @Timeout(30)
  void concurrentReviewCreatesCannotProduceDuplicates() throws Exception {
    UUID id = book(PATIENT, 1);
    time(VISIT_END);
    report(id, DOCTOR1).andExpect(status().isCreated());
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var tasks = new ArrayList<java.util.concurrent.Future<Integer>>();
      for (int index = 0; index < 2; index++) {
        tasks.add(executor.submit(() -> {
          barrier.await(5, TimeUnit.SECONDS);
          return review(id, "post", PATIENT, "PATIENT", REVIEW).andReturn().getResponse()
            .getStatus();
        }));
      }
      var statuses = new ArrayList<Integer>();
      for (var task : tasks) {
        statuses.add(task.get(15, TimeUnit.SECONDS));
      }
      assertThat(statuses).containsExactlyInAnyOrder(201, 409);
      assertThat(reviews.count()).isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void eligibleNonauthorDoctorSeesOnlyCurrentHospitalReports() throws Exception {
    UUID hospital2Report = book(PATIENT, 1);
    UUID hospital1Report = book(PATIENT, 2);
    time(VISIT_END);
    report(hospital2Report, DOCTOR1).andExpect(status().isCreated());
    report(hospital1Report, DOCTOR2).andExpect(status().isCreated());
    addDoctor3();
    var relationship = relationship(OffsetDateTime.parse("2026-11-05T09:00:00+03:00"));
    UUID patientId = patients.findByAccountId(UUID.fromString(PATIENT)).orElseThrow().getId();
    mvc.perform(
        get(path(hospital1Report, "report")).header("Authorization", bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isOk());
    mvc.perform(
        get(path(hospital2Report, "report")).header("Authorization", bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isNotFound());
    mvc.perform(get("/api/patients/" + patientId + "/reports").header("Authorization",
        bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
      .andExpect(jsonPath("$.content[0].hospitalId").value(1));
    mvc.perform(post(path(relationship.getId(), "cancel")).header("Authorization",
        bearer(PATIENT, "PATIENT")))
      .andExpect(status().isNoContent());
    mvc.perform(
        get(path(hospital1Report, "report")).header("Authorization", bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isNotFound());
    // Policy fixture: a completed relationship remains qualifying indefinitely.
    var completed = new Appointment(patients.findById(patientId).orElseThrow(), 3, 1,
      OffsetDateTime.parse("2026-11-05T09:00:00+03:00"),
      OffsetDateTime.parse("2026-11-05T09:30:00+03:00"), BOOKING_TIME);
    completed.complete(UUID.fromString(DOCTOR3), Instant.parse("2026-11-05T06:30:00Z"));
    appointments.saveAndFlush(completed);
    time(Instant.parse("2028-01-01T00:00:00Z"));
    mvc.perform(
        get(path(hospital1Report, "report")).header("Authorization", bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isOk());
  }

  @Test
  void pastBookedAndNoShowRelationshipsDoNotGrantReportAccess() throws Exception {
    UUID id = book(PATIENT, 2);
    time(VISIT_END);
    report(id, DOCTOR2).andExpect(status().isCreated());
    addDoctor3();
    relationship(OffsetDateTime.parse("2026-11-05T09:00:00+03:00"));
    time(Instant.parse("2026-11-05T06:00:00Z"));
    mvc.perform(get(path(id, "report")).header("Authorization", bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isNotFound());
    var link = appointments.findAll().stream().filter(a -> a.getDoctorId() == 3).findFirst()
      .orElseThrow();
    time(Instant.parse("2026-11-05T06:30:00Z"));
    mvc.perform(
        post(path(link.getId(), "no-show")).header("Authorization", bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isNoContent());
    mvc.perform(get(path(id, "report")).header("Authorization", bearer(DOCTOR3, "DOCTOR")))
      .andExpect(status().isNotFound());
  }

  @Test
  void currentHospitalChangeAndDirectoryFailureCannotExposeMedicalContent() throws Exception {
    UUID id = book(PATIENT, 1);
    time(VISIT_END);
    DOCTOR1_HOSPITAL.set(1);
    report(id, DOCTOR1).andExpect(status().isNotFound());
    mvc.perform(post(path(id, "no-show")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isNotFound());
    DOCTOR1_HOSPITAL.set(2);
    report(id, DOCTOR1).andExpect(status().isCreated());
    DOCTOR1_HOSPITAL.set(1);
    mvc.perform(get(path(id, "report")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isNotFound());
    DOCTOR1_HOSPITAL.set(2);
    OFFLINE.set(true);
    mvc.perform(get(path(id, "report")).header("Authorization", bearer(DOCTOR1, "DOCTOR")))
      .andExpect(status().isServiceUnavailable());
    mvc.perform(get(path(id, "report")).header("Authorization", bearer(PATIENT, "PATIENT")))
      .andExpect(status().isOk());
    SecurityContextHolder.clearContext();
    assertThatThrownBy(() -> visitService.submitReport(id, new ReportInputDto("a", "b", "c")))
      .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  private UUID book(String account, long doctor) throws Exception {
    if (!patients.existsByAccountId(UUID.fromString(account))) {
      mvc.perform(post("/api/patients/me").header("Authorization", bearer(account, "PATIENT"))
        .contentType(MediaType.APPLICATION_JSON).content("""
          {"firstName":"Test","lastName":"Patient","birthDate":"1990-01-01"}
          """)).andExpect(status().isCreated());
    }
    var response = mvc.perform(
        post("/api/appointments").header("Authorization", bearer(account, "PATIENT"))
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"doctorId\":%d,\"start\":\"2026-11-04T09:00:00+03:00\"}".formatted(doctor)))
      .andExpect(status().isCreated()).andReturn().getResponse();
    return UUID.fromString(json.readTree(response.getContentAsString()).get("id").asText());
  }

  private ResultActions report(UUID id, String doctor) throws Exception {
    return mvc.perform(post(path(id, "report")).header("Authorization", bearer(doctor, "DOCTOR"))
      .contentType(MediaType.APPLICATION_JSON).content(REPORT));
  }

  private ResultActions review(UUID id, String method, String account, String role, String body)
    throws Exception {
    var request = method.equals("post") ? post(path(id, "review")) : put(path(id, "review"));
    return mvc.perform(request.header("Authorization", bearer(account, role))
      .contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private Appointment relationship(OffsetDateTime start) {
    // A policy fixture for doctor 3; does not add/edit production seeds or schedules.
    var patient = patients.findByAccountId(UUID.fromString(PATIENT)).orElseThrow();
    return appointments.saveAndFlush(
      new Appointment(patient, 3, 1, start, start.plusMinutes(30), BOOKING_TIME));
  }

  private void addDoctor3() {
    new JdbcTemplate(dataSource).update(
      "INSERT INTO doctor_accounts(account_id,doctor_id) VALUES (?,3)",
      UUID.fromString(DOCTOR3));
  }

  private void time(Instant instant) {
    ((MutableClock) clock).set(instant);
  }

  private static String path(UUID id, String action) {
    return "/api/appointments/" + id + "/" + action;
  }

  private static String bearer(String subject, String role) {
    var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY)));
    var claims = JwtClaimsSet.builder().subject(subject).issuer(ISSUER)
      .audience(List.of("hospital-api"))
      .issuedAt(Instant.now().minusSeconds(10)).expiresAt(Instant.now().plusSeconds(300))
      .claim("roles", List.of(role)).build();
    return "Bearer " + encoder.encode(JwtEncoderParameters.from(
      JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
  }

  private static RSAKey key() {
    try {
      return new RSAKeyGenerator(2048).generate();
    } catch (JOSEException exception) {
      throw new IllegalStateException(exception);
    }
  }

  static class MutableClock extends Clock {

    private final AtomicReference<Instant> time = new AtomicReference<>(BOOKING_TIME);

    void set(Instant value) {
      time.set(value);
    }

    @Override
    public Instant instant() {
      return time.get();
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(instant(), zone);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Configuration {

    @Bean
    @Primary
    Clock testClock() {
      return new MutableClock();
    }

    @Bean
    @Primary
    DirectoryClient testDirectory() {
      return doctor -> {
        if (OFFLINE.get()) {
          throw new DirectoryUnavailableException("private failure");
        }
        if (doctor < 1 || doctor > 3) {
          throw new ResourceNotFoundException("Doctor was not found");
        }
        return new DirectoryDoctorDto(doctor,
          new DirectoryDoctorDto.Hospital(doctor == 1 ? DOCTOR1_HOSPITAL.get() : 1));
      };
    }

    @Bean
    @Primary
    JwtDecoder testDecoder() throws JOSEException {
      var decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
      decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(ISSUER), new AccountTokenValidator()));
      return decoder;
    }
  }
}
