//package ru.otus.hw.web;
//
//import java.net.URI;
//import java.util.List;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.context.annotation.Primary;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import ru.otus.hw.web.security.SessionAccessTokens;
//import org.springframework.web.util.UriComponentsBuilder;
//import ru.otus.hw.web.client.DirectoryClient;
//import ru.otus.hw.web.dto.*;
//import ru.otus.hw.web.exception.DirectoryUnavailableException;
//import ru.otus.hw.web.exception.DoctorNotFoundException;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.hamcrest.Matchers.containsString;
//import static org.hamcrest.Matchers.not;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest(properties = "app.web.client-secret=test-client-secret-for-web")
//@AutoConfigureMockMvc
//@Import(WebApplicationTest.DirectoryStubConfiguration.class)
//class WebApplicationTest {
//
//  @MockitoBean
//  SessionAccessTokens tokens;
//  @Autowired
//  MockMvc mvc;
//  @Autowired
//  StubDirectory directory;
//
//  @BeforeEach
//  void reset() {
//    directory.unavailable = false;
//    directory.empty = false;
//    directory.search = null;
//  }
//
//  @Test
//  void publicPagesRenderWithoutSigningIn() throws Exception {
//    mvc.perform(get("/")).andExpect(status().isOk())
//      .andExpect(content().string(containsString("Найдите подходящего врача")));
//    mvc.perform(get("/doctors")).andExpect(status().isOk())
//      .andExpect(content().string(containsString("Cardiology")));
//    mvc.perform(get("/doctors/1")).andExpect(status().isOk())
//      .andExpect(content().string(containsString("University")));
//    mvc.perform(get("/login?error")).andExpect(status().isOk())
//      .andExpect(content().string(containsString("Не удалось выполнить вход")));
//    mvc.perform(get("/account")).andExpect(status().is3xxRedirection())
//      .andExpect(redirectedUrl("http://localhost/login"));
//  }
//
//  @Test
//  void filtersUseQueryParametersAndHistoryRestoreGetsFullPage() throws Exception {
//    mvc.perform(get("/doctors").queryParam("hospitalId", "2").queryParam("specialtyIds", "3", "4"))
//      .andExpect(status().isOk());
//    assertThat(directory.search).isEqualTo(new DoctorSearchInput(2L, List.of(3L, 4L)));
//    mvc.perform(get("/doctors").header("HX-Request", "true"))
//      .andExpect(status().isOk())
//      .andExpect(content().string(containsString("id=\"doctor-results\"")))
//      .andExpect(content().string(not(containsString("<html"))));
//    mvc.perform(
//        get("/doctors").header("HX-Request", "true").header("HX-History-Restore-Request", "true"))
//      .andExpect(status().isOk()).andExpect(content().string(containsString("<html")));
//  }
//
//  @Test
//  void invalidFiltersAndMissingDoctorsHaveUsefulStatuses() throws Exception {
//    mvc.perform(get("/doctors").queryParam("hospitalId", "-1")).andExpect(status().isBadRequest());
//    mvc.perform(get("/doctors").queryParam("specialtyIds", "0")).andExpect(status().isBadRequest());
//    mvc.perform(get("/doctors").queryParam("hospitalId", "not-a-number"))
//      .andExpect(status().isBadRequest());
//    mvc.perform(get("/doctors").queryParam("hospitalId", "")).andExpect(status().isOk());
//    mvc.perform(get("/doctors").queryParam("specialtyIds",
//        java.util.Collections.nCopies(21, "1").toArray(String[]::new)))
//      .andExpect(status().isBadRequest());
//    mvc.perform(get("/doctors/0")).andExpect(status().isBadRequest());
//    mvc.perform(get("/doctors/999")).andExpect(status().isNotFound());
//  }
//
//  @Test
//  void anOutageIsNotAnEmptySearchAndHtmxGetsAnErrorFragment() throws Exception {
//    directory.empty = true;
//    mvc.perform(get("/doctors")).andExpect(status().isOk())
//      .andExpect(content().string(containsString("Врачи по выбранным условиям не найдены")));
//    directory.unavailable = true;
//    mvc.perform(get("/doctors")).andExpect(status().isServiceUnavailable())
//      .andExpect(content().string(not(containsString("Врачи по выбранным условиям не найдены"))));
//    mvc.perform(get("/doctors").header("HX-Request", "true"))
//      .andExpect(status().isServiceUnavailable()).andExpect(header().string("HX-Push-Url", "false"))
//      .andExpect(content().string(containsString("временно недоступен")))
//      .andExpect(content().string(not(containsString("<html"))));
//  }
//
//  @Test
//  void directoryTextIsEscaped() throws Exception {
//    mvc.perform(get("/doctors")).andExpect(status().isOk())
//      .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
//      .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
//  }
//
//  @Test
//  void roleAwarePagesContainNoTokens() throws Exception {
//    for (String role : List.of("PATIENT", "DOCTOR", "ADMIN")) {
//      String heading = switch (role) {
//        case "PATIENT" -> "Patient account";
//        case "DOCTOR" -> "Doctor account";
//        default -> "Administrator account";
//      };
//      if (role.equals("PATIENT")) {
//        mvc.perform(
//            get("/account").with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))))
//          .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/patient/profile"));
//        continue;
//      }
//      mvc.perform(get("/account").with(oidcLogin().idToken(t -> t.tokenValue("secret-id-token"))
//          .authorities(new SimpleGrantedAuthority("ROLE_" + role))))
//        .andExpect(status().isOk()).andExpect(content().string(containsString(heading)))
//        .andExpect(content().string(containsString("Выйти")))
//        .andExpect(content().string(not(containsString("secret-id-token"))));
//    }
//  }
//
//  @Test
//  void authorizationRequestUsesStatePkceAndRegisteredCallback() throws Exception {
//    var response = mvc.perform(get("/oauth2/authorization/hospital"))
//      .andExpect(status().is3xxRedirection()).andReturn().getResponse();
//    URI redirect = URI.create(response.getRedirectedUrl());
//    assertThat(redirect.getHost()).isEqualTo("127.0.0.1");
//    assertThat(redirect.getPort()).isEqualTo(9000);
//    var query = UriComponentsBuilder.fromUri(redirect).build().getQueryParams();
//    assertThat(query.getFirst("response_type")).isEqualTo("code");
//    assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
//    assertThat(query.getFirst("code_challenge")).isNotBlank();
//    assertThat(query.getFirst("state")).isNotBlank();
//    assertThat(java.net.URLDecoder.decode(query.getFirst("redirect_uri"),
//      java.nio.charset.StandardCharsets.UTF_8))
//      .isEqualTo("http://127.0.0.1:8080/login/oauth2/code/hospital");
//  }
//
//  @Test
//  void logoutRequiresCsrfAndUsesOidcProvider() throws Exception {
//    mvc.perform(post("/logout").with(oidcLogin())).andExpect(status().isForbidden());
//    var response = mvc.perform(post("/logout").with(csrf()).with(oidcLogin()
//        .clientRegistration(ClientRegistrationFixture.registration())))
//      .andExpect(status().is3xxRedirection()).andReturn().getResponse();
//    URI redirect = URI.create(response.getRedirectedUrl());
//    assertThat(redirect.toString()).startsWith("http://127.0.0.1:9000/connect/logout?");
//    var query = UriComponentsBuilder.fromUri(redirect).build().getQueryParams();
//    assertThat(java.net.URLDecoder.decode(query.getFirst("post_logout_redirect_uri"),
//      java.nio.charset.StandardCharsets.UTF_8))
//      .isEqualTo("http://127.0.0.1:8080/");
//    assertThat(response.getHeaders("Set-Cookie")).anyMatch(
//      c -> c.startsWith("WEBSESSION=") && c.contains("Max-Age=0"));
//  }
//
//  @Test
//  void assetsAreLocalAndAvailable() throws Exception {
//    for (String path : List.of("/css/app.css", "/js/app.js",
//      "/webjars/htmx.org/2.0.10/dist/htmx.min.js")) {
//      mvc.perform(get(path)).andExpect(status().isOk());
//    }
//  }
//
//  @TestConfiguration(proxyBeanMethods = false)
//  static class DirectoryStubConfiguration {
//
//    @Bean
//    @Primary
//    StubDirectory stubDirectory() {
//      return new StubDirectory();
//    }
//  }
//
//  static class StubDirectory implements DirectoryClient {
//
//    boolean unavailable;
//    boolean empty;
//    DoctorSearchInput search;
//    final HospitalDto hospital = new HospitalDto(2, "Central Hospital", "Main Street");
//    final SpecialtyDto specialty = new SpecialtyDto(3, "Cardiology");
//    final DoctorDto doctor = new DoctorDto(1, "Alex", "<script>alert(1)</script>", "University", 10,
//      "doctor@example.test", hospital, List.of(specialty));
//
//    void check() {
//      if (unavailable) {
//        throw new DirectoryUnavailableException();
//      }
//    }
//
//    public List<HospitalDto> hospitals() {
//      check();
//      return List.of(hospital);
//    }
//
//    public List<SpecialtyDto> specialties() {
//      check();
//      return List.of(specialty);
//    }
//
//    public List<DoctorDto> doctors(DoctorSearchInput input) {
//      check();
//      search = input;
//      return empty ? List.of() : List.of(doctor);
//    }
//
//    public DoctorDto doctor(long id) {
//      check();
//      if (id != 1) {
//        throw new DoctorNotFoundException();
//      }
//      return doctor;
//    }
//  }
//}
