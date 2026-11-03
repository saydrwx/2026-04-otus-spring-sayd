package ru.otus.hw.directory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.otus.hw.directory.dto.*;
import ru.otus.hw.directory.repository.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// No @Transactional on the test: request transactions must really commit, including FK/unique errors.
// Every test rebuilds fixtures inside this class's disposable container, never an external database.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Testcontainers
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
class DirectoryIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
    System.getProperty("directory.test.postgres-image", "postgres:17-alpine"));
  static final TestJwtIssuer TOKENS = new TestJwtIssuer();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("AUTH_ISSUER", () -> TestJwtIssuer.ISSUER);
    registry.add("AUTH_JWK_SET_URI", TOKENS::jwkSetUri);
  }

  @AfterAll
  static void closeIssuer() {
    TOKENS.close();
  }

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper json;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  DoctorRepository doctors;
  @Autowired
  EntityManagerFactory entityManagerFactory;
  long alphaHospital, zuluHospital, neuro, cardio, general, zoe, amyFirst, amySecond, bea;

  @BeforeEach
  void fixtures() {
    // Flyway runs the existing schema and seeds once at startup. These fixtures reset test data only.
    jdbc.execute(
      "truncate table doctors_specialties, doctors, specialties, hospitals restart identity cascade");
    zuluHospital = hospital("Zulu Hospital");
    alphaHospital = hospital("Alpha Hospital");
    neuro = specialty("Neurology");
    cardio = specialty("Cardiology");
    general = specialty("General medicine");
    zoe = doctor("Zoe", "Same", alphaHospital, neuro, cardio);
    amyFirst = doctor("Amy", "Same", alphaHospital, neuro);
    amySecond = doctor("Amy", "Same", zuluHospital, cardio);
    bea = doctor("Bea", "Able", zuluHospital, general);
  }

  long hospital(String name) {
    return jdbc.queryForObject("insert into hospitals(name,address) values(?,?) returning id",
      Long.class, name, "Main Street");
  }

  long specialty(String name) {
    return jdbc.queryForObject("insert into specialties(name) values(?) returning id", Long.class,
      name);
  }

  long doctor(String firstName, String lastName, long hospital, long... specialtyIds) {
    long id = jdbc.queryForObject("""
      insert into doctors(first_name,last_name,experience_years,hospital_id)
      values(?,?,0,?) returning id
      """, Long.class, firstName, lastName, hospital);
    for (long specialty : specialtyIds) {
      jdbc.update("insert into doctors_specialties(doctor_id,specialty_id) values(?,?)", id,
        specialty);
    }
    return id;
  }

  MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
    return request.header("Authorization", TOKENS.bearer("ADMIN"));
  }

  ResultActions write(MockHttpServletRequestBuilder request, Object body) throws Exception {
    return mvc.perform(
      admin(request).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
  }

  JsonNode response(ResultActions action) throws Exception {
    return json.readTree(action.andReturn().getResponse().getContentAsByteArray());
  }

  JsonNode read(String path) throws Exception {
    return response(mvc.perform(get(path)).andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)));
  }

  List<Long> ids(JsonNode array) {
    var result = new java.util.ArrayList<Long>();
    array.forEach(item -> result.add(item.path("id").longValue()));
    return result;
  }

  DoctorInputDto input(long hospital, Set<Long> specialties) {
    return new DoctorInputDto("  Иван  ", "  Петров  ", "  University  ", 0,
      "doctor@example.test", hospital, specialties);
  }

  @Test
  void publicListsAreOrderedAndDoctorsHaveCompleteNestedDtosWithOpenInViewDisabled()
    throws Exception {
    assertThat(ids(read("/api/hospitals"))).containsExactly(alphaHospital, zuluHospital);
    assertThat(ids(read("/api/specialties"))).containsExactly(cardio, general, neuro);
    var result = read("/api/doctors");
    assertThat(ids(result)).containsExactly(bea, amyFirst, amySecond, zoe);
    var doctor = read("/api/doctors/" + zoe);
    assertThat(doctor.path("hospital").path("id").asLong()).isEqualTo(alphaHospital);
    assertThat(ids(doctor.path("specialties"))).containsExactly(cardio, neuro);
    assertThat(doctor.has(
      "hospitalId")).isFalse(); // Public contract is nested hospital, not internal entity fields.
  }

  @Test
  void hospitalFilterAndSpecialtyOrFilterIntersectAndNeverDuplicateDoctors() throws Exception {
    var result = response(mvc.perform(get("/api/doctors")
        .param("hospitalId", Long.toString(alphaHospital))
        .param("specialtyIds", Long.toString(neuro), Long.toString(cardio)))
      .andExpect(status().isOk()));
    assertThat(ids(result)).containsExactly(amyFirst, zoe);
    var either = response(mvc.perform(
        get("/api/doctors").param("specialtyIds", Long.toString(neuro), Long.toString(cardio)))
      .andExpect(status().isOk()));
    assertThat(ids(either)).containsExactly(amyFirst, amySecond, zoe);
  }

  @Test
  void filteringByOneSpecialtyDoesNotTrimOtherSpecialtiesFromFetchedDoctor() throws Exception {
    var result = response(
      mvc.perform(get("/api/doctors").param("specialtyIds", Long.toString(neuro)))
        .andExpect(status().isOk()));
    assertThat(ids(result)).containsExactly(amyFirst, zoe);
    assertThat(ids(result.get(1).path("specialties"))).containsExactly(cardio, neuro);
  }

  @Test
  void unknownPositiveFiltersYieldEmptyListsRatherThanMissingResourceErrors() throws Exception {
    assertThat(ids(response(mvc.perform(get("/api/doctors").param("hospitalId", "999999"))
      .andExpect(status().isOk())))).isEmpty();
    assertThat(ids(response(mvc.perform(get("/api/doctors").param("specialtyIds", "999999"))
      .andExpect(status().isOk())))).isEmpty();
  }

  @Test
  void hospitalCrudCommitsNormalizedValuesAndDeleteIs204() throws Exception {
    var created = write(post("/api/hospitals"),
      new HospitalInputDto("  New Hospital  ", "  New Address  "))
      .andExpect(status().isCreated());
    long id = response(created).path("id").asLong();
    assertThat(id).isPositive();
    created.andExpect(header().string("Location", "/api/hospitals/" + id));
    assertThat(read("/api/hospitals/" + id).path("name").asText()).isEqualTo("New Hospital");
    write(put("/api/hospitals/" + id), new HospitalInputDto("  Renamed  ", "  Changed  "))
      .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    assertThat(read("/api/hospitals/" + id).path("address").asText()).isEqualTo("Changed");
    mvc.perform(admin(delete("/api/hospitals/" + id))).andExpect(status().isNoContent())
      .andExpect(content().string(""));
    mvc.perform(get("/api/hospitals/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void specialtyCrudCommitsNormalizedValuesAndDeleteIs204() throws Exception {
    var created = write(post("/api/specialties"), new SpecialtyInputDto("  New Specialty  "))
      .andExpect(status().isCreated());
    long id = response(created).path("id").asLong();
    created.andExpect(header().string("Location", "/api/specialties/" + id));
    assertThat(read("/api/specialties/" + id).path("name").asText()).isEqualTo("New Specialty");
    write(put("/api/specialties/" + id), new SpecialtyInputDto("  Renamed specialty  "))
      .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed specialty"));
    mvc.perform(admin(delete("/api/specialties/" + id))).andExpect(status().isNoContent());
    mvc.perform(get("/api/specialties/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void doctorCrudReplacesHospitalAndSpecialtyLinksInsteadOfAccumulatingThem() throws Exception {
    var created = write(post("/api/doctors"), input(alphaHospital, Set.of(neuro, cardio)))
      .andExpect(status().isCreated());
    long id = response(created).path("id").asLong();
    created.andExpect(header().string("Location", "/api/doctors/" + id));
    var saved = read("/api/doctors/" + id);
    assertThat(saved.path("firstName").asText()).isEqualTo("Иван");
    assertThat(saved.path("lastName").asText()).isEqualTo("Петров");
    assertThat(saved.path("education").asText()).isEqualTo("University");
    assertThat(saved.path("email").asText()).isEqualTo("doctor@example.test");
    assertThat(ids(saved.path("specialties"))).containsExactly(cardio, neuro);
    write(put("/api/doctors/" + id),
      new DoctorInputDto("Alex", "New", null, 5, null, zuluHospital, Set.of(general)))
      .andExpect(status().isOk());
    var updated = read("/api/doctors/" + id);
    assertThat(updated.path("hospital").path("id").asLong()).isEqualTo(zuluHospital);
    assertThat(ids(updated.path("specialties"))).containsExactly(general);
    assertThat(updated.path("education").isNull()).isTrue();
    assertThat(updated.path("email").isNull()).isTrue();
    assertThat(
      jdbc.queryForObject("select count(*) from doctors_specialties where doctor_id=?", Long.class,
        id)).isEqualTo(1);
    mvc.perform(admin(delete("/api/doctors/" + id))).andExpect(status().isNoContent());
    assertThat(
      jdbc.queryForObject("select count(*) from doctors_specialties where doctor_id=?", Long.class,
        id)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from hospitals where id=?", Long.class,
      zuluHospital)).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from specialties where id=?", Long.class,
      general)).isEqualTo(1);
    mvc.perform(get("/api/doctors/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void maximumDtoLengthsFitDatabaseColumns() throws Exception {
    String name = "n".repeat(255);
    var hospital = response(
      write(post("/api/hospitals"), new HospitalInputDto(name, "a".repeat(300)))
        .andExpect(status().isCreated()));
    long hospitalId = hospital.path("id").asLong();
    var specialty = response(write(post("/api/specialties"), new SpecialtyInputDto(name))
      .andExpect(status().isCreated()));
    var doctor = response(
      write(post("/api/doctors"), new DoctorInputDto(name, name, "e".repeat(1000), 0,
        null, hospitalId, Set.of(specialty.path("id").asLong())))
        .andExpect(status().isCreated()));
    var stored = read("/api/doctors/" + doctor.path("id").asLong());
    assertThat(stored.path("firstName").asText()).hasSize(255);
    assertThat(stored.path("lastName").asText()).hasSize(255);
    assertThat(stored.path("education").asText()).hasSize(1000);
    assertThat(stored.path("hospital").path("address").asText()).hasSize(300);
  }

  @Test
  void optionalDoctorValuesCanBeNullAndZeroExperienceIsAccepted() throws Exception {
    var created = write(post("/api/doctors"),
      new DoctorInputDto("Alex", "Smith", null, 0, null, alphaHospital, Set.of(neuro)))
      .andExpect(status().isCreated());
    var result = response(created);
    assertThat(result.path("experienceYears").asInt()).isZero();
    assertThat(result.path("education").isNull()).isTrue();
    assertThat(result.path("email").isNull()).isTrue();
  }

  @Test
  void nonexistentDoctorHospitalAndSpecialtyReturn404AndLeaveNoPartialCreate() throws Exception {
    for (String catalog : List.of("hospitals", "specialties", "doctors")) {
      mvc.perform(get("/api/" + catalog + "/999999")).andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
      mvc.perform(admin(delete("/api/" + catalog + "/999999"))).andExpect(status().isNotFound());
    }
    write(put("/api/hospitals/999999"), new HospitalInputDto("Missing", "Address")).andExpect(
      status().isNotFound());
    write(put("/api/specialties/999999"), new SpecialtyInputDto("Missing")).andExpect(
      status().isNotFound());
    write(post("/api/doctors"), input(999999, Set.of(neuro))).andExpect(status().isNotFound());
    write(post("/api/doctors"), input(alphaHospital, Set.of(neuro, 999999L))).andExpect(
      status().isNotFound());
    assertThat(jdbc.queryForObject("select count(*) from doctors", Long.class)).isEqualTo(4);
    assertThat(
      jdbc.queryForObject("select count(*) from doctors_specialties", Long.class)).isEqualTo(5);
  }

  @Test
  void invalidUpdateReferenceRollsBackScalarAndAssociationChanges() throws Exception {
    JsonNode before = read("/api/doctors/" + zoe);
    write(put("/api/doctors/" + zoe), input(zuluHospital, Set.of(general, 999999L)))
      .andExpect(status().isNotFound());
    assertThat(read("/api/doctors/" + zoe)).isEqualTo(before);
    write(put("/api/doctors/" + zoe), input(999999, Set.of(general)))
      .andExpect(status().isNotFound());
    assertThat(read("/api/doctors/" + zoe)).isEqualTo(before);
    write(put("/api/doctors/999999"), input(alphaHospital, Set.of(neuro))).andExpect(
      status().isNotFound());
  }

  @Test
  void duplicateSpecialtyCreateAndUpdateReturnSanitized409AndRollback() throws Exception {
    assertSanitizedConflict(
      write(post("/api/specialties"), new SpecialtyInputDto("  Neurology  ")));
    assertSanitizedConflict(
      write(put("/api/specialties/" + general), new SpecialtyInputDto("Neurology")));
    assertThat(read("/api/specialties/" + general).path("name").asText()).isEqualTo(
      "General medicine");
    assertThat(jdbc.queryForObject("select count(*) from specialties", Long.class)).isEqualTo(3);
  }

  @Test
  void referencedHospitalCannotBeDeletedAndConflictDoesNotLeakSql() throws Exception {
    assertSanitizedConflict(mvc.perform(admin(delete("/api/hospitals/" + alphaHospital))));
    assertThat(read("/api/hospitals/" + alphaHospital).path("id").asLong()).isEqualTo(
      alphaHospital);
    assertThat(read("/api/doctors/" + zoe).path("hospital").path("id").asLong()).isEqualTo(
      alphaHospital);
  }

  void assertSanitizedConflict(ResultActions result) throws Exception {
    var node = response(result.andExpect(status().isConflict())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)));
    assertThat(node.path("status").asInt()).isEqualTo(409);
    assertThat(node.path("detail").asText()).isEqualTo("Data integrity violation");
    assertThat(node.toString()).doesNotContain("org.postgresql", "constraint",
      "specialties_name_key", "doctors_hospital_id_fkey", "insert into", "delete from");
  }

  @Test
  void deletingSpecialtyCascadesJoinRowsButKeepsDoctorsAndOtherSpecialties() throws Exception {
    mvc.perform(admin(delete("/api/specialties/" + neuro))).andExpect(status().isNoContent());
    assertThat(ids(read("/api/doctors/" + zoe).path("specialties"))).containsExactly(cardio);
    assertThat(ids(read("/api/doctors/" + amyFirst).path("specialties"))).isEmpty();
    assertThat(jdbc.queryForObject("select count(*) from doctors", Long.class)).isEqualTo(4);
    assertThat(jdbc.queryForObject("select count(*) from doctors_specialties where specialty_id=?",
      Long.class, neuro)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from specialties where id=?", Long.class,
      cardio)).isEqualTo(1);
  }

  @Test
  void entityGraphsFetchBothAssociationsWithoutAnNPlusOneRead() {
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    var values = doctors.findAll(DoctorSpecifications.matching(null, List.of()),
      Sort.by("lastName", "firstName", "id"));
    assertThat(values).hasSize(4);
    for (var value : values) {
      assertThat(Hibernate.isInitialized(value.getHospital())).isTrue();
      assertThat(Hibernate.isInitialized(value.getSpecialties())).isTrue();
    }
    // One joined query is expected; tolerate one extra framework query, but never one per doctor.
    assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);
  }

  @Test
  void findByIdEntityGraphAlsoInitializesHospitalAndSpecialties() {
    var value = doctors.findById(zoe).orElseThrow();
    assertThat(Hibernate.isInitialized(value.getHospital())).isTrue();
    assertThat(Hibernate.isInitialized(value.getSpecialties())).isTrue();
    assertThat(value.getSpecialties()).hasSize(2);
  }

  @Test
  void databaseRejectsNegativeExperienceMissingHospitalAndDuplicateJoin() throws Exception {
    assertThatThrownBy(() -> jdbc.update("update doctors set experience_years=-1 where id=?", zoe))
      .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("update doctors set hospital_id=999999 where id=?", zoe))
      .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
      () -> jdbc.update("insert into doctors_specialties(doctor_id,specialty_id) values(?,?)", zoe,
        neuro))
      .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(read("/api/doctors/" + zoe).path("experienceYears").asInt()).isZero();
  }

  @Test
  void realSignedAdminJwtAuthorizesWritesWithoutCsrfAndDoesNotCreateSession() throws Exception {
    var action = write(post("/api/specialties"), new SpecialtyInputDto("Signed token specialty"))
      .andExpect(status().isCreated()).andExpect(header().doesNotExist("Set-Cookie"));
    assertThat(action.andReturn().getRequest().getSession(false)).isNull();
  }

  @Test
  void realPatientDoctorAndScopeOnlyTokensCannotWrite() throws Exception {
    for (String role : List.of("PATIENT", "DOCTOR")) {
      mvc.perform(post("/api/specialties").header("Authorization", TOKENS.bearer(role))
          .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Forbidden\"}"))
        .andExpect(status().isForbidden());
    }
    String scopeOnly = TOKENS.bearer(claims -> claims.claims(map -> {
      map.remove("roles");
      map.put("scope", "ADMIN");
    }));
    mvc.perform(post("/api/specialties").header("Authorization", scopeOnly)
        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Forbidden\"}"))
      .andExpect(status().isForbidden());
    assertThat(jdbc.queryForObject("select count(*) from specialties", Long.class)).isEqualTo(3);
  }

  @Test
  void malformedAndBadSignatureTokensAreRejectedEvenOnOtherwisePublicRead() throws Exception {
    String token = TOKENS.bearer("ADMIN");
    int index = token.lastIndexOf('.') + 1;
    String tampered =
      token.substring(0, index) + (token.charAt(index) == 'A' ? 'B' : 'A') + token.substring(
        index + 1);
    for (String bad : List.of("Bearer not-a-jwt", tampered)) {
      mvc.perform(get("/api/doctors").header("Authorization", bad))
        .andExpect(status().isUnauthorized());
    }
    mvc.perform(get("/api/doctors")).andExpect(status().isOk());
  }
}
