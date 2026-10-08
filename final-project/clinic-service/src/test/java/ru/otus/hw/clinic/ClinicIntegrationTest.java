package ru.otus.hw.clinic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
import ru.otus.hw.clinic.exception.DirectoryUnavailableException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.model.AppointmentStatus;
import ru.otus.hw.clinic.model.Patient;
import ru.otus.hw.clinic.repository.AppointmentRepository;
import ru.otus.hw.clinic.repository.PatientRepository;
import ru.otus.hw.clinic.security.AccountTokenValidator;
import ru.otus.hw.clinic.service.PatientService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real PostgreSQL + production Flyway migrations + JPA + service method security + MVC.
// Tokens are genuinely signed; only the JWKS network lookup is replaced with a local public key.
@SpringBootTest(properties = "spring.jackson.deserialization.fail-on-unknown-properties=true")
@AutoConfigureMockMvc
@Testcontainers
@Import(ClinicIntegrationTest.TokenConfiguration.class)
class ClinicIntegrationTest {
  private static final String ISSUER = "http://127.0.0.1:9000";
  private static final String PATIENT = "00000000-0000-0000-0000-000000000004";
  private static final String DOCTOR = "00000000-0000-0000-0000-000000000002";
  private static final String BODY = """
    {"firstName":" Anna ","lastName":"Petrova","middleName":null,"birthDate":"1990-05-20"}
    """;
  private static final RSAKey KEY = newKey();

  private static final java.nio.file.Path DOCUMENT_ROOT = temporaryDocumentRoot();

  private static java.nio.file.Path temporaryDocumentRoot() {
    try { return java.nio.file.Files.createTempDirectory("clinic-regression-documents-"); }
    catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
  }

  @org.junit.jupiter.api.AfterAll
  static void removeDocumentRoot() throws java.io.IOException {
    org.springframework.util.FileSystemUtils.deleteRecursively(DOCUMENT_ROOT);
  }

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("app.documents.root", () -> DOCUMENT_ROOT.toString());
    registry.add("app.documents.cleanup-enabled", () -> "false");
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("AUTH_ISSUER", () -> ISSUER);
  }

  @Autowired MockMvc mvc;
  @Autowired PatientRepository patients;
  @Autowired AppointmentRepository appointments;
  @Autowired ObjectMapper json;
  @Autowired Clock clock;
  private static final AtomicBoolean DIRECTORY_OFFLINE =
    new AtomicBoolean();
  @Autowired DataSource dataSource;
  @Autowired PatientService patientService;

  @BeforeEach
  void cleanPatients() {
    appointments.deleteAllInBatch();
    patients.deleteAll();
    ((AdjustableClock) clock).set(Instant.parse("2026-11-02T06:00:00Z"));
    DIRECTORY_OFFLINE.set(false);
  }

  @Test
  void onboardingThenReadAndUpdate() throws Exception {
    String token = token(PATIENT, "PATIENT");
    mvc.perform(get("/api/patients/me").header("Authorization", token))
      .andExpect(status().isNotFound());
    mvc.perform(put("/api/patients/me").header("Authorization", token)
      .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isNotFound());
    mvc.perform(post("/api/patients/me").header("Authorization", token)
        .contentType(MediaType.APPLICATION_JSON).content(BODY))
      .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/patients/me"))
      .andExpect(jsonPath("$.id").isNotEmpty()).andExpect(jsonPath("$.firstName").value("Anna"));
    mvc.perform(get("/api/patients/me").header("Authorization", token))
      .andExpect(status().isOk()).andExpect(jsonPath("$.birthDate").value("1990-05-20"));
    mvc.perform(put("/api/patients/me").header("Authorization", token)
        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("Petrova", "Ivanova")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.lastName").value("Ivanova"));
    assertThat(patients.findByAccountId(UUID.fromString(PATIENT))).isPresent();
  }

  @Test
  void anotherPatientCannotReadOrUpdateTheFirstPatientsProfile() throws Exception {
    String first = token(PATIENT, "PATIENT");
    String otherId = "10000000-0000-0000-0000-000000000001";
    String second = token(otherId, "PATIENT");
    mvc.perform(post("/api/patients/me").header("Authorization", first)
      .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());
    mvc.perform(get("/api/patients/me").header("Authorization", second))
      .andExpect(status().isNotFound());
    mvc.perform(put("/api/patients/me").header("Authorization", second)
      .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isNotFound());
    // Unknown fields are rejected instead of allowing ownership reassignment.
    mvc.perform(post("/api/patients/me").header("Authorization", second)
      .contentType(MediaType.APPLICATION_JSON)
      .content(BODY.replace("{", "{\"accountId\":\"" + PATIENT + "\",")))
      .andExpect(status().isBadRequest());
    assertThat(patients.count()).isEqualTo(1);
  }

  @Test
  void duplicateCreationReturnsSafeConflict() throws Exception {
    String token = token(PATIENT, "PATIENT");
    mvc.perform(post("/api/patients/me").header("Authorization", token)
      .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());
    mvc.perform(post("/api/patients/me").header("Authorization", token)
      .contentType(MediaType.APPLICATION_JSON).content(BODY))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Patient profile already exists"));
    assertThat(patients.count()).isEqualTo(1);
  }

  @Test
  void uniqueConstraintAlsoProtectsConcurrentCreation() throws Exception {
    // Bypass the friendly service precheck to exercise the race's database safety net.
    var jdbc = new JdbcTemplate(dataSource);
    jdbc.update("INSERT INTO patients(id,account_id,first_name,last_name,birth_date) VALUES (?,?,?, ?,?)",
      UUID.randomUUID(), UUID.fromString(PATIENT), "Anna", "Petrova", java.sql.Date.valueOf("1990-05-20"));
    assertThatThrownBy(() -> jdbc.update(
      "INSERT INTO patients(id,account_id,first_name,last_name,birth_date) VALUES (?,?,?, ?,?)",
      UUID.randomUUID(), UUID.fromString(PATIENT), "Other", "Name", java.sql.Date.valueOf("1990-05-20")))
      .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsInvalidInput() throws Exception {
    for (String body : List.of(BODY.replace(" Anna ", " "),
      BODY.replace("1990-05-20", "2990-05-20"), BODY.replace("\"1990-05-20\"", "null"),
      BODY.replace("Petrova", "x".repeat(101)))) {
      mvc.perform(post("/api/patients/me").header("Authorization", token(PATIENT, "PATIENT"))
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    assertThat(patients.count()).isZero();
  }

  @Test
  void doctorsAndAdminCannotAccessPatientProfile() throws Exception {
    for (String role : List.of("DOCTOR", "ADMIN")) {
      for (var method : List.of(get("/api/patients/me"), post("/api/patients/me"), put("/api/patients/me"))) {
        mvc.perform(method.header("Authorization", token(DOCTOR, role))
          .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
      }
    }
  }

  @Test
  void resolvesBothSeededDoctorLinksAndMissingLink() throws Exception {
    mvc.perform(get("/api/doctors/me").header("Authorization", token(DOCTOR, "DOCTOR")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.doctorId").value(1));
    mvc.perform(get("/api/doctors/me").header("Authorization",
      token("00000000-0000-0000-0000-000000000003", "DOCTOR")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.doctorId").value(2));
    mvc.perform(get("/api/doctors/me").header("Authorization", token(PATIENT, "DOCTOR")))
      .andExpect(status().isNotFound());
    mvc.perform(get("/api/doctors/me").header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isForbidden());
  }

  @Test
  void rejectsMissingAndMalformedBearerTokens() throws Exception {
    mvc.perform(get("/api/patients/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/patients/me").header("Authorization", "Bearer garbage"))
      .andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void rejectsWrongIssuerAudienceExpiredSubjectAndSignature() throws Exception {
    for (String bearer : List.of(
      signed(PATIENT, "PATIENT", "wrong-issuer", "hospital-api", Instant.now().plusSeconds(300), KEY),
      signed(PATIENT, "PATIENT", ISSUER, "hospital-web", Instant.now().plusSeconds(300), KEY),
      signed(PATIENT, "PATIENT", ISSUER, null, Instant.now().plusSeconds(300), KEY),
      signed(PATIENT, "PATIENT", ISSUER, "hospital-api", Instant.now().minusSeconds(120), KEY),
      token("email@example.com", "PATIENT"),
      signed(PATIENT, "PATIENT", ISSUER, "hospital-api", Instant.now().plusSeconds(300), newKey()))) {
      mvc.perform(get("/api/patients/me").header("Authorization", bearer))
        .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void optimisticLockingRejectsAStaleDetachedUpdate() {
    var patient = new Patient(UUID.fromString(PATIENT));
    patient.setFirstName("Anna");
    patient.setLastName("Petrova");
    patient.setBirthDate(LocalDate.of(1990, 5, 20));
    var saved = patients.saveAndFlush(patient);
    var first = patients.findById(saved.getId()).orElseThrow();
    var stale = patients.findById(saved.getId()).orElseThrow();
    first.setLastName("Ivanova");
    patients.saveAndFlush(first);
    stale.setLastName("Stale");
    assertThatThrownBy(() -> patients.saveAndFlush(stale))
      .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    assertThat(patients.findById(saved.getId()).orElseThrow().getLastName()).isEqualTo("Ivanova");
  }

  @Test
  void serviceCannotBeCalledWithoutAuthentication() {
    SecurityContextHolder.clearContext();
    assertThatThrownBy(() -> patientService.findCurrent())
      .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  void publicScheduleReturnsOrderedSeededDays() throws Exception {
    mvc.perform(get("/api/doctors/1/schedule"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.doctorId").value(1))
      .andExpect(jsonPath("$.hospitalId").value(2)).andExpect(jsonPath("$.timezone").value("+03:00"))
      .andExpect(jsonPath("$.slotDurationMinutes").value(30))
      .andExpect(jsonPath("$.workingDays.length()").value(5))
      .andExpect(jsonPath("$.workingDays[0].dayOfWeek").value("MONDAY"))
      .andExpect(jsonPath("$.workingDays[4].dayOfWeek").value("FRIDAY"))
      .andExpect(jsonPath("$.workingDays[0].startTime").value("09:00:00"))
      .andExpect(jsonPath("$.workingDays[0].endTime").value("17:00:00"));
  }

  @Test
  void publicAvailabilityUsesTheSameStoredSchedule() throws Exception {
    mvc.perform(get("/api/doctors/1/availability").param("date", "2026-11-03"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.slots.length()").value(16))
      .andExpect(jsonPath("$.slots[0].start").value("2026-11-03T09:00:00+03:00"))
      .andExpect(jsonPath("$.slots[15].end").value("2026-11-03T17:00:00+03:00"));
    mvc.perform(get("/api/doctors/2/schedule")).andExpect(status().isOk())
      .andExpect(jsonPath("$.hospitalId").value(1)).andExpect(jsonPath("$.workingDays.length()").value(5));
  }

  @Test
  void noScheduleAndWeekendAndOutsideWindowProduceEmptyLists() throws Exception {
    mvc.perform(get("/api/doctors/3/schedule")).andExpect(status().isOk())
      .andExpect(jsonPath("$.workingDays").isEmpty());
    mvc.perform(get("/api/doctors/3/availability").param("date", "2026-11-03"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.slots").isEmpty());
    for (String date : List.of("2026-11-07", "2026-11-02", "2026-11-17")) {
      mvc.perform(get("/api/doctors/1/availability").param("date", date))
        .andExpect(status().isOk()).andExpect(jsonPath("$.slots").isEmpty());
    }
  }

  @Test
  void invalidAvailabilityInputsAreBadRequests() throws Exception {
    mvc.perform(get("/api/doctors/1/availability")).andExpect(status().isBadRequest());
    for (String date : List.of("invalid", "2026-02-30")) {
      mvc.perform(get("/api/doctors/1/availability").param("date", date))
        .andExpect(status().isBadRequest());
    }
    mvc.perform(get("/api/doctors/0/schedule")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/doctors/-1/availability").param("date", "2026-11-03"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void missingProviderAndUnavailableDirectoryAreDifferentErrors() throws Exception {
    mvc.perform(get("/api/doctors/999/schedule")).andExpect(status().isNotFound());
    mvc.perform(get("/api/doctors/998/availability").param("date", "2026-11-03"))
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.detail").value("Directory is temporarily unavailable; please retry later"));
    // Even out-of-window requests validate the provider rather than inventing an empty success.
    mvc.perform(get("/api/doctors/998/availability").param("date", "2020-01-01"))
      .andExpect(status().isServiceUnavailable());
  }

  @Test
  void publicReadPermissionsDoNotPermitAnonymousWrites() throws Exception {
    mvc.perform(post("/api/doctors/1/schedule").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/doctors/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void scheduleDatabaseConstraintsRejectMisalignmentAndDuplicates() {
    var jdbc = new JdbcTemplate(dataSource);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM weekly_schedules", Long.class)).isEqualTo(10L);
    String sql = "INSERT INTO weekly_schedules(doctor_id,day_of_week,start_time,end_time) VALUES (?,?,?,?)";
    assertThatThrownBy(() -> jdbc.update(sql, 1, 1, java.sql.Time.valueOf("09:00:00"), java.sql.Time.valueOf("17:00:00")))
      .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update(sql, 100, 1, java.sql.Time.valueOf("09:15:00"), java.sql.Time.valueOf("17:00:00")))
      .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update(sql, 100, 8, java.sql.Time.valueOf("09:00:00"), java.sql.Time.valueOf("17:00:00")))
      .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update(sql, 100, 1, java.sql.Time.valueOf("11:00:00"), java.sql.Time.valueOf("19:00:00")))
      .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static final String OTHER_PATIENT = "10000000-0000-0000-0000-000000000001";
  private static final String ADMIN = "00000000-0000-0000-0000-000000000001";
  private static final String SECOND_DOCTOR = "00000000-0000-0000-0000-000000000003";
  private static final String WEDNESDAY_START = "2026-11-04T09:00:00+03:00";

  @Test
  void bookingHidesSlotAndCancellationReleasesItWithoutDeletingHistory() throws Exception {
    createProfile(PATIENT);
    var created = book(PATIENT, 1, WEDNESDAY_START).andExpect(status().isCreated()).andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
    mvc.perform(get("/api/doctors/1/availability").param("date", "2026-11-04"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.slots.length()").value(15))
      .andExpect(jsonPath("$.slots[0].start").value("2026-11-04T09:30:00+03:00"));
    mvc.perform(get("/api/appointments/" + id).header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BOOKED"))
      .andExpect(jsonPath("$.hospitalId").value(2));
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isNoContent());
    mvc.perform(get("/api/doctors/1/availability").param("date", "2026-11-04"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.slots.length()").value(16));
    mvc.perform(get("/api/appointments/" + id).header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"))
      .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
    book(PATIENT, 1, WEDNESDAY_START).andExpect(status().isCreated());
    assertThat(appointments.count()).isEqualTo(2);
  }

  @Test
  void duplicateSlotIsConflictAndDifferentDoctorsMayOverlap() throws Exception {
    createProfile(PATIENT);
    book(PATIENT, 1, WEDNESDAY_START).andExpect(status().isCreated());
    mvc.perform(post("/api/appointments").header("Authorization", token(PATIENT, "PATIENT"))
      .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(1, WEDNESDAY_START)))
      .andExpect(status().isConflict());
    book(PATIENT, 2, WEDNESDAY_START).andExpect(status().isCreated());
    assertThat(appointments.count()).isEqualTo(2);
  }

  @Test
  void rejectsMissingProfileAndInvalidSlotsWithoutCreatingAppointments() throws Exception {
    mvc.perform(post("/api/appointments").header("Authorization", token(PATIENT, "PATIENT"))
      .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(1, WEDNESDAY_START)))
      .andExpect(status().isConflict());
    createProfile(PATIENT);
    for (String start : List.of("2026-11-04T09:15:00+03:00", "2026-11-04T09:00:00.000001+03:00",
      "2026-11-04T17:00:00+03:00", "2026-11-07T09:00:00+03:00",
      "2026-11-02T09:00:00+03:00", "2026-11-17T09:00:00+03:00")) {
      mvc.perform(post("/api/appointments").header("Authorization", token(PATIENT, "PATIENT"))
        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(1, start)))
        .andExpect(status().isBadRequest());
    }
    mvc.perform(post("/api/appointments").header("Authorization", token(PATIENT, "PATIENT"))
      .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(3, WEDNESDAY_START)))
      .andExpect(status().isBadRequest());
    assertThat(appointments.count()).isZero();
  }

  @Test
  void bookingAcceptsExactWindowEdgesAndEquivalentOffsets() throws Exception {
    createProfile(PATIENT);
    book(PATIENT, 1, "2026-11-03T06:00:00Z").andExpect(status().isCreated())
      .andExpect(jsonPath("$.start").value("2026-11-03T09:00:00+03:00"));
    book(PATIENT, 1, "2026-11-16T09:00:00+03:00").andExpect(status().isCreated());
  }

  @Test
  void requestValidationDoesNotAllowChoosingAnotherPatient() throws Exception {
    createProfile(PATIENT);
    String body = appointmentBody(1, WEDNESDAY_START)
      .replace("{", "{\"patientId\":\"" + UUID.randomUUID() + "\",");
    mvc.perform(post("/api/appointments").header("Authorization", token(PATIENT, "PATIENT"))
      .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    for (String invalid : List.of("{}", "{\"doctorId\":0,\"start\":null}",
      "{\"doctorId\":1,\"start\":\"2026-11-04T09:00:00\"}")) {
      mvc.perform(post("/api/appointments").header("Authorization", token(PATIENT, "PATIENT"))
        .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
    }
    assertThat(appointments.count()).isZero();
  }

  @Test
  void appointmentReadsAndCancellationEnforceOwnershipAndDoctorAssignment() throws Exception {
    createProfile(PATIENT);
    createProfile(OTHER_PATIENT);
    String id = bookedId(PATIENT, 1);
    mvc.perform(get("/api/appointments/" + id).header("Authorization", token(OTHER_PATIENT, "PATIENT")))
      .andExpect(status().isNotFound());
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(OTHER_PATIENT, "PATIENT")))
      .andExpect(status().isNotFound());
    mvc.perform(get("/api/appointments/" + id).header("Authorization", token(DOCTOR, "DOCTOR")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").doesNotExist())
      .andExpect(jsonPath("$.birthDate").doesNotExist());
    mvc.perform(get("/api/appointments/" + id).header("Authorization", token(SECOND_DOCTOR, "DOCTOR")))
      .andExpect(status().isNotFound());
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(DOCTOR, "DOCTOR")))
      .andExpect(status().isForbidden());
    mvc.perform(get("/api/appointments/" + id).header("Authorization", token(ADMIN, "ADMIN")))
      .andExpect(status().isOk());
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(ADMIN, "ADMIN")))
      .andExpect(status().isNoContent());
  }

  @Test
  void listingIsPagedAndScopedByAuthenticatedIdentity() throws Exception {
    createProfile(PATIENT);
    createProfile(OTHER_PATIENT);
    bookedId(PATIENT, 1);
    bookedId(OTHER_PATIENT, 2);
    for (String role : List.of("PATIENT", "DOCTOR")) {
      String account = role.equals("PATIENT") ? PATIENT : DOCTOR;
      mvc.perform(get("/api/appointments").header("Authorization", token(account, role)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].doctorId").value(1));
    }
    mvc.perform(get("/api/appointments").param("size", "1").header("Authorization", token(ADMIN, "ADMIN")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
      .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.totalPages").value(2));
    mvc.perform(get("/api/appointments").param("page", "1").param("size", "1")
      .header("Authorization", token(ADMIN, "ADMIN"))).andExpect(status().isOk())
      .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.content.length()").value(1));
    mvc.perform(get("/api/appointments").param("size", "101").header("Authorization", token(ADMIN, "ADMIN")))
      .andExpect(status().isBadRequest());
    mvc.perform(get("/api/appointments").param("page", "-1").header("Authorization", token(ADMIN, "ADMIN")))
      .andExpect(status().isBadRequest());
  }

  @Test
  void bookingIsPatientOnlyAndNeverAnonymous() throws Exception {
    for (String role : List.of("DOCTOR", "ADMIN")) {
      mvc.perform(post("/api/appointments").header("Authorization", token(DOCTOR, role))
        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(1, WEDNESDAY_START)))
        .andExpect(status().isForbidden());
    }
    mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
      .content(appointmentBody(1, WEDNESDAY_START))).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/appointments")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/appointments/" + UUID.randomUUID() + "/cancel"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void directoryFailuresDoNotCreateBookingsAndLocalReadsAndCancellationStillWorkOffline() throws Exception {
    createProfile(PATIENT);
    for (long doctor : List.of(999L, 998L)) {
      mvc.perform(post("/api/appointments").header("Authorization", token(PATIENT, "PATIENT"))
        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(doctor, WEDNESDAY_START)))
        .andExpect(status().is(doctor == 999 ? 404 : 503));
    }
    assertThat(appointments.count()).isZero();
    String id = bookedId(PATIENT, 1);
    DIRECTORY_OFFLINE.set(true);
    mvc.perform(get("/api/appointments/" + id).header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isOk());
    mvc.perform(get("/api/appointments").header("Authorization", token(ADMIN, "ADMIN")))
      .andExpect(status().isOk());
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isNoContent());
    mvc.perform(get("/api/doctors/1/availability").param("date", "2026-11-04"))
      .andExpect(status().isServiceUnavailable());
  }

  @Test
  void cancellationAtStartIsRejectedAndRepeatCancellationPreservesItsAuditFields() throws Exception {
    createProfile(PATIENT);
    String id = bookedId(PATIENT, 1);
    ((AdjustableClock) clock).set(Instant.parse("2026-11-04T06:00:00Z"));
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isConflict());
    ((AdjustableClock) clock).set(Instant.parse("2026-11-04T05:59:59.999999Z"));
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(PATIENT, "PATIENT")))
      .andExpect(status().isNoContent());
    var firstCancellation = appointments.findById(UUID.fromString(id)).orElseThrow().getCancelledAt();
    ((AdjustableClock) clock).set(Instant.parse("2026-11-05T06:00:00Z"));
    mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(ADMIN, "ADMIN")))
      .andExpect(status().isNoContent());
    assertThat(appointments.findById(UUID.fromString(id)).orElseThrow().getCancelledAt()).isEqualTo(firstCancellation);
  }

  @Test
  void completedAndNoShowAppointmentsCannotBeCancelled() throws Exception {
    createProfile(PATIENT);
    String id = bookedId(PATIENT, 1);
    var jdbc = new JdbcTemplate(dataSource);
    for (String state : List.of("COMPLETED", "NO_SHOW")) {
      // State fixture for cancellation checks; real outcome commands have their own clinical tests.
      jdbc.update("UPDATE appointments SET status = ?, outcome_at = ?, outcome_by = ? WHERE id = ?",
        state, java.sql.Timestamp.from(appointments.findById(UUID.fromString(id)).orElseThrow().getEndAt().toInstant()),
        UUID.fromString(DOCTOR), UUID.fromString(id));
      mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", token(ADMIN, "ADMIN")))
        .andExpect(status().isConflict());
      mvc.perform(get("/api/doctors/1/availability").param("date", "2026-11-04"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.slots.length()").value(15));
    }
  }

  @Test
  @Timeout(30)
  void competingPatientsCannotBothReserveTheSameSlot() throws Exception {
    createProfile(PATIENT);
    createProfile(OTHER_PATIENT);
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var tasks = new ArrayList<Future<Integer>>();
      for (String account : List.of(PATIENT, OTHER_PATIENT)) {
        tasks.add(executor.submit(() -> {
          barrier.await(5, TimeUnit.SECONDS);
          return mvc.perform(post("/api/appointments").header("Authorization", token(account, "PATIENT"))
            .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(1, WEDNESDAY_START)))
            .andReturn().getResponse().getStatus();
        }));
      }
      var statuses = new ArrayList<Integer>();
      for (var task : tasks) {
        statuses.add(task.get(15, TimeUnit.SECONDS));
      }
      assertThat(statuses).containsExactlyInAnyOrder(201, 409);
      assertThat(appointments.count()).isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  @Timeout(30)
  void simultaneousOwnerAndAdminCancellationAreIdempotent() throws Exception {
    createProfile(PATIENT);
    String id = bookedId(PATIENT, 1);
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var results = new ArrayList<Future<Integer>>();
      for (String bearer : List.of(token(PATIENT, "PATIENT"), token(ADMIN, "ADMIN"))) {
        results.add(executor.submit(() -> {
          barrier.await(5, TimeUnit.SECONDS);
          return mvc.perform(post("/api/appointments/" + id + "/cancel").header("Authorization", bearer))
            .andReturn().getResponse().getStatus();
        }));
      }
      for (var result : results) {
        assertThat(result.get(15, TimeUnit.SECONDS)).isEqualTo(204);
      }
      var appointment = appointments.findById(UUID.fromString(id)).orElseThrow();
      assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
      assertThat(appointment.getCancelledBy()).isIn(UUID.fromString(PATIENT), UUID.fromString(ADMIN));
    } finally {
      executor.shutdownNow();
    }
  }

  private void createProfile(String account) throws Exception {
    mvc.perform(post("/api/patients/me").header("Authorization", token(account, "PATIENT"))
      .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());
  }

  private ResultActions book(String account, long doctor, String start)
    throws Exception {
    return mvc.perform(post("/api/appointments").header("Authorization", token(account, "PATIENT"))
      .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(doctor, start)));
  }

  private String bookedId(String account, long doctor) throws Exception {
    var response = book(account, doctor, WEDNESDAY_START).andExpect(status().isCreated()).andReturn();
    return json.readTree(response.getResponse().getContentAsString()).get("id").asText();
  }

  private static String appointmentBody(long doctor, String start) {
    return "{\"doctorId\":%d,\"start\":\"%s\"}".formatted(doctor, start);
  }

  static class AdjustableClock extends Clock {
    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    AdjustableClock(Instant instant) {
      this(new AtomicReference<>(instant), ZoneOffset.UTC);
    }

    private AdjustableClock(AtomicReference<Instant> instant, ZoneId zone) {
      this.instant = instant;
      this.zone = zone;
    }

    void set(Instant value) { instant.set(value); }
    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId value) { return new AdjustableClock(instant, value); }
    @Override public Instant instant() { return instant.get(); }
  }

  private static String token(String subject, String role) {
    return signed(subject, role, ISSUER, "hospital-api", Instant.now().plusSeconds(300), KEY);
  }

  private static String signed(String subject, String role, String issuer, String audience,
                               Instant expires, RSAKey key) {
    var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    var claimsBuilder = JwtClaimsSet.builder().subject(subject).issuer(issuer)
      .issuedAt(Instant.now().minusSeconds(300)).expiresAt(expires).claim("roles", List.of(role));
    if (audience != null) {
      claimsBuilder.audience(List.of(audience));
    }
    return "Bearer " + encoder.encode(JwtEncoderParameters.from(
      JwsHeader.with(SignatureAlgorithm.RS256).build(), claimsBuilder.build())).getTokenValue();
  }

  private static RSAKey newKey() {
    try {
      return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
    } catch (JOSEException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TokenConfiguration {
    @Bean
    @Primary
    Clock fixedClock() {
      return new AdjustableClock(Instant.parse("2026-11-02T06:00:00Z"));
    }

    @Bean
    @Primary
    DirectoryClient stubDirectoryClient() {
      return doctorId -> {
        if (DIRECTORY_OFFLINE.get() || doctorId == 998) {
          throw new DirectoryUnavailableException("private upstream details");
        }
        if (doctorId < 1 || doctorId > 3) {
          throw new ResourceNotFoundException("Doctor was not found");
        }
        return new DirectoryDoctorDto(doctorId,
          new DirectoryDoctorDto.Hospital(doctorId == 1 ? 2 : 1));
      };
    }

    @Bean
    @Primary
    JwtDecoder localJwtDecoder() throws JOSEException {
      var decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
      decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(ISSUER), new AccountTokenValidator()));
      return decoder;
    }
  }
}
