package ru.otus.hw.directory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.otus.hw.directory.controller.*;
import ru.otus.hw.directory.dto.*;
import ru.otus.hw.directory.exception.*;
import ru.otus.hw.directory.security.SecurityConfiguration;
import ru.otus.hw.directory.service.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Controller/validation/authorization checks only: no database, Docker or auth server.
@WebMvcTest({HospitalController.class, SpecialtyController.class, DoctorController.class})
@Import(SecurityConfiguration.class)
class DirectoryWebTest {

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper json;
  @MockitoBean(name = "directoryJwtDecoder")
  JwtDecoder decoder;
  @MockitoBean
  HospitalService hospitals;
  @MockitoBean
  SpecialtyService specialties;
  @MockitoBean
  DoctorService doctors;

  static final HospitalDto HOSPITAL = new HospitalDto(10L, "Central", "Main Street");
  static final SpecialtyDto SPECIALTY = new SpecialtyDto(20L, "Cardiology");
  static final DoctorDto DOCTOR = new DoctorDto(30L, "Alex", "Smith", null, 0, null,
    HOSPITAL, List.of(SPECIALTY));

  @BeforeEach
  void setup() {
    when(hospitals.findAll()).thenReturn(List.of(HOSPITAL));
    when(specialties.findAll()).thenReturn(List.of(SPECIALTY));
    when(doctors.findAll(any(), any())).thenReturn(List.of(DOCTOR));
  }

  MockHttpServletRequestBuilder role(String role, MockHttpServletRequestBuilder request) {
    return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  ObjectNode doctorBody() {
    return json.valueToTree(new DoctorInputDto("Alex", "Smith", null, 0, null, 10L, Set.of(20L)));
  }

  @Test
  void listsArePublicAndFiltersArePassedWithoutChangingTheirMeaning() throws Exception {
    mvc.perform(get("/api/hospitals")).andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(10));
    mvc.perform(get("/api/specialties")).andExpect(status().isOk())
      .andExpect(jsonPath("$[0].name").value("Cardiology"));
    mvc.perform(get("/api/doctors").param("hospitalId", "10").param("specialtyIds", "20", "21"))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].hospital.id").value(10));
    verify(doctors).findAll(10L, List.of(20L, 21L));
    verifyNoInteractions(decoder);
  }

  @Test
  void anonymousWritesRequireAuthenticationForAllCatalogs() throws Exception {
    for (String catalog : List.of("hospitals", "specialties", "doctors")) {
      mvc.perform(post("/api/" + catalog).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
      mvc.perform(
          put("/api/" + catalog + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
      mvc.perform(delete("/api/" + catalog + "/1")).andExpect(status().isUnauthorized());
    }
    verifyNoInteractions(hospitals, specialties, doctors);
  }

  @Test
  void patientAndDoctorRolesCannotMutateAnyCatalogEvenWithARoleField() throws Exception {
    for (String role : List.of("PATIENT", "DOCTOR")) {
      for (String catalog : List.of("hospitals", "specialties", "doctors")) {
        mvc.perform(role(role, post("/api/" + catalog)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"ADMIN\"}"))
          .andExpect(status().isForbidden());
        mvc.perform(
            role(role, put("/api/" + catalog + "/1")).contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());
        mvc.perform(role(role, delete("/api/" + catalog + "/1"))).andExpect(status().isForbidden());
      }
    }
    verifyNoInteractions(hospitals, specialties, doctors);
  }

  @Test
  void adminCreateUses201LocationAndValidatedDtoWithoutSessionCsrf() throws Exception {
    when(hospitals.create(any())).thenReturn(HOSPITAL);
    when(specialties.create(any())).thenReturn(SPECIALTY);
    when(doctors.create(any())).thenReturn(DOCTOR);
    mvc.perform(role("ADMIN", post("/api/hospitals")).contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Central\",\"address\":\"Main Street\"}"))
      .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/hospitals/10"));
    mvc.perform(role("ADMIN", post("/api/specialties")).contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Cardiology\"}"))
      .andExpect(status().isCreated())
      .andExpect(header().string("Location", "/api/specialties/20"));
    mvc.perform(role("ADMIN", post("/api/doctors")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(doctorBody())))
      .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/doctors/30"));
    verify(doctors).create(new DoctorInputDto("Alex", "Smith", null, 0, null, 10L, Set.of(20L)));
  }

  @Test
  void updateUsesPutAndDeleteReturns204WithNoBody() throws Exception {
    when(hospitals.update(eq(10L), any())).thenReturn(HOSPITAL);
    when(specialties.update(eq(20L), any())).thenReturn(SPECIALTY);
    when(doctors.update(eq(30L), any())).thenReturn(DOCTOR);
    mvc.perform(role("ADMIN", put("/api/hospitals/10")).contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Central\",\"address\":\"Main Street\"}"))
      .andExpect(status().isOk());
    mvc.perform(role("ADMIN", put("/api/specialties/20")).contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Cardiology\"}"))
      .andExpect(status().isOk());
    mvc.perform(role("ADMIN", put("/api/doctors/30")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(doctorBody())))
      .andExpect(status().isOk());
    for (String path : List.of("hospitals/10", "specialties/20", "doctors/30")) {
      mvc.perform(role("ADMIN", delete("/api/" + path)))
        .andExpect(status().isNoContent()).andExpect(content().string(""));
    }
    verify(hospitals).deleteById(10L);
    verify(specialties).deleteById(20L);
    verify(doctors).deleteById(30L);
  }

  static Stream<Arguments> invalidDoctorFields() {
    return Stream.of(
      Arguments.of("firstName", " "), Arguments.of("lastName", ""),
      Arguments.of("firstName", null), Arguments.of("lastName", null),
      Arguments.of("firstName", "x".repeat(256)), Arguments.of("lastName", "x".repeat(256)),
      Arguments.of("education", "x".repeat(1001)), Arguments.of("experienceYears", -1),
      Arguments.of("experienceYears", null), Arguments.of("email", "not-an-email"),
      Arguments.of("email", "a".repeat(250) + "@example.test"),
      Arguments.of("hospitalId", 0), Arguments.of("hospitalId", null),
      Arguments.of("specialtyIds", null), Arguments.of("specialtyIds", List.of()),
      Arguments.of("specialtyIds", List.of(0)),
      Arguments.of("specialtyIds", java.util.Arrays.asList((Long) null)));
  }

  @ParameterizedTest(name = "{index}: invalid doctor field {0}")
  @MethodSource("invalidDoctorFields")
  void doctorValidationStopsBeforeService(String field, Object value) throws Exception {
    var body = doctorBody();
    body.set(field, json.valueToTree(value));
    mvc.perform(role("ADMIN", post("/api/doctors")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(body)))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.violations").isMap());
    verifyNoInteractions(doctors, hospitals, specialties);
  }

  static Stream<Arguments> invalidHospitalFields() {
    return Stream.of(Arguments.of("name", " "), Arguments.of("name", "x".repeat(256)),
      Arguments.of("address", ""), Arguments.of("address", "x".repeat(301)));
  }

  @ParameterizedTest(name = "invalid hospital field: {0}")
  @MethodSource("invalidHospitalFields")
  void hospitalValidationStopsBeforeService(String field, String value) throws Exception {
    ObjectNode body = json.createObjectNode().put("name", "Central").put("address", "Main Street");
    body.put(field, value);
    mvc.perform(role("ADMIN", post("/api/hospitals")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(body)))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.violations", hasKey(field)));
    verifyNoInteractions(hospitals);
  }

  @Test
  void specialtyValidationAndUpdateValidationAlsoRunAtControllerBoundary() throws Exception {
    for (String name : List.of(" ", "x".repeat(256))) {
      mvc.perform(role("ADMIN", post("/api/specialties")).contentType(MediaType.APPLICATION_JSON)
          .content(json.writeValueAsBytes(new SpecialtyInputDto(name))))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.violations.name").exists());
    }
    mvc.perform(role("ADMIN", put("/api/doctors/30")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(doctorBody().put("experienceYears", -1))))
      .andExpect(status().isBadRequest());
    verifyNoInteractions(specialties, doctors);
  }

  @Test
  void missingRequiredFieldsAreRejectedAndValuesAreNotEchoedInViolations() throws Exception {
    mvc.perform(
        role("ADMIN", post("/api/hospitals")).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.violations.name").exists())
      .andExpect(jsonPath("$.violations.address").exists());
    mvc.perform(role("ADMIN", post("/api/doctors")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(doctorBody().put("email", "rejected-marker@"))))
      .andExpect(status().isBadRequest())
      .andExpect(content().string(not(containsString("rejected-marker"))));
    verifyNoInteractions(hospitals, doctors);
  }

  @Test
  void malformedJsonUnsupportedContentTypeAndNonNumericIdsAreDistinctClientErrors()
    throws Exception {
    mvc.perform(
        role("ADMIN", post("/api/hospitals")).contentType(MediaType.APPLICATION_JSON).content("{"))
      .andExpect(status().isBadRequest());
    mvc.perform(
        role("ADMIN", post("/api/hospitals")).contentType(MediaType.TEXT_PLAIN).content("not-json"))
      .andExpect(status().isUnsupportedMediaType());
    mvc.perform(get("/api/doctors/not-a-number")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/doctors").param("hospitalId", "bad")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/doctors").param("specialtyIds", "bad"))
      .andExpect(status().isBadRequest());
    mvc.perform(
        role("ADMIN", patch("/api/doctors/30")).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isMethodNotAllowed());
    verifyNoInteractions(hospitals, doctors);
  }

  @Test
  void missingResourcesUseProblemDetailAndIntegrityErrorsHideDatabaseStructure() throws Exception {
    when(hospitals.findById(99L)).thenThrow(new HospitalNotFoundException(99L));
    when(specialties.findById(99L)).thenThrow(new SpecialtyNotFoundException(99L));
    when(doctors.findById(99L)).thenThrow(new DoctorNotFoundException(99L));
    for (String path : List.of("hospitals/99", "specialties/99", "doctors/99")) {
      mvc.perform(get("/api/" + path)).andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
    doThrow(new org.springframework.dao.DataIntegrityViolationException(
      "secret_table secret_constraint SQL details"))
      .when(hospitals).deleteById(10L);
    mvc.perform(role("ADMIN", delete("/api/hospitals/10"))).andExpect(status().isConflict())
      .andExpect(jsonPath("$.detail").value("Data integrity violation"))
      .andExpect(content().string(not(containsString("secret_"))));
  }

  @Test
  void unmappedPathsAreDeniedRatherThanAccidentallyExposed() throws Exception {
    mvc.perform(get("/internal/debug")).andExpect(status().isUnauthorized());
    mvc.perform(role("ADMIN", get("/internal/debug"))).andExpect(status().isForbidden());
    verifyNoInteractions(hospitals, specialties, doctors);
  }
}
