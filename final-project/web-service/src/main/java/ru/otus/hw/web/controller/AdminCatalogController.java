package ru.otus.hw.web.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.AdminException;
import ru.otus.hw.web.security.AdminSession;
import ru.otus.hw.web.service.AdminService;
import ru.otus.hw.web.service.DirectoryService;

@RequiredArgsConstructor
@Controller
public class AdminCatalogController {

  private final DirectoryService directory;
  private final AdminService admin;
  private final AdminSession session;

  @InitBinder("hospitalForm")
  void hospitalBinding(WebDataBinder b) {
    FormBindings.only(b, "name", "address");
  }

  @InitBinder("specialtyForm")
  void specialtyBinding(WebDataBinder b) {
    FormBindings.only(b, "name");
  }

  @InitBinder("doctorForm")
  void doctorBinding(WebDataBinder b) {
    FormBindings.only(b, "firstName", "lastName", "education", "experienceYears", "email",
      "hospitalId", "specialtyIds");
  }

  @GetMapping("/admin/catalog/{kind}")
  public String list(@PathVariable String kind, Model model) {
    var catalog = CatalogKind.parse(kind);
    model.addAttribute("kind", catalog.path());
    model.addAttribute("rows", rows(catalog));
    return "admin/catalog";
  }

  @GetMapping({"/admin/catalog/{kind}/new", "/admin/catalog/{kind}/{id}/edit"})
  public String form(@PathVariable String kind, @PathVariable(required = false) @Positive Long id,
    Model model) {
    var catalog = CatalogKind.parse(kind);
    Object form;
    switch (catalog) {
      case HOSPITALS -> {
        var value = new HospitalAdminForm();
        if (id != null) {
          var item = directory.hospitals().stream().filter(h -> h.id() == id).findFirst()
            .orElseThrow(this::missing);
          value.setName(item.name());
          value.setAddress(item.address());
        }
        form = value;
      }
      case SPECIALTIES -> {
        var value = new SpecialtyAdminForm();
        if (id != null) {
          var item = directory.specialties().stream().filter(h -> h.id() == id).findFirst()
            .orElseThrow(this::missing);
          value.setName(item.name());
        }
        form = value;
      }
      case DOCTORS -> {
        var value = new DoctorAdminForm();
        if (id != null) {
          var item = directory.doctor(id);
          value.setFirstName(item.firstName());
          value.setLastName(item.lastName());
          value.setEducation(item.education());
          value.setExperienceYears(item.experienceYears());
          value.setEmail(item.email());
          value.setHospitalId(item.hospital().id());
          value.setSpecialtyIds(item.specialties().stream().map(SpecialtyDto::id)
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)));
        }
        form = value;
      }
      default -> throw missing();
    }
    model.addAttribute(catalog == CatalogKind.HOSPITALS ? "hospitalForm"
      : catalog == CatalogKind.SPECIALTIES ? "specialtyForm" : "doctorForm", form);
    return prepare(catalog, id, model);
  }

  @PostMapping(value = {"/admin/catalog/hospitals/new",
    "/admin/catalog/hospitals/{id}/edit"}, consumes = "application/x-www-form-urlencoded")
  public String saveHospital(@PathVariable(required = false) @Positive Long id,
    @Valid @ModelAttribute("hospitalForm") HospitalAdminForm form,
    BindingResult errors, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    if (errors.getSuppressedFields().length > 0) {
      errors.reject("unsupported", "Форма содержит неподдерживаемые поля.");
    }
    if (errors.hasErrors()) {
      response.setStatus(400);
      return prepare(CatalogKind.HOSPITALS, id, model);
    }
    session.call(user, request, response,
      token -> admin.save(token, CatalogKind.HOSPITALS, id, form.input()));
    return "redirect:/admin/catalog/hospitals";
  }

  @PostMapping(value = {"/admin/catalog/specialties/new",
    "/admin/catalog/specialties/{id}/edit"}, consumes = "application/x-www-form-urlencoded")
  public String saveSpecialty(@PathVariable(required = false) @Positive Long id,
    @Valid @ModelAttribute("specialtyForm") SpecialtyAdminForm form,
    BindingResult errors, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    if (errors.getSuppressedFields().length > 0) {
      errors.reject("unsupported", "Форма содержит неподдерживаемые поля.");
    }
    if (errors.hasErrors()) {
      response.setStatus(400);
      return prepare(CatalogKind.SPECIALTIES, id, model);
    }
    session.call(user, request, response,
      token -> admin.save(token, CatalogKind.SPECIALTIES, id, form.input()));
    return "redirect:/admin/catalog/specialties";
  }

  @PostMapping(value = {"/admin/catalog/doctors/new",
    "/admin/catalog/doctors/{id}/edit"}, consumes = "application/x-www-form-urlencoded")
  public String saveDoctor(@PathVariable(required = false) @Positive Long id,
    @Valid @ModelAttribute("doctorForm") DoctorAdminForm form,
    BindingResult errors, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    if (errors.getSuppressedFields().length > 0) {
      errors.reject("unsupported", "Форма содержит неподдерживаемые поля.");
    }
    if (errors.hasErrors()) {
      response.setStatus(400);
      return prepare(CatalogKind.DOCTORS, id, model);
    }
    session.call(user, request, response,
      token -> admin.save(token, CatalogKind.DOCTORS, id, form.input()));
    return "redirect:/admin/catalog/doctors";
  }

  @GetMapping("/admin/catalog/{kind}/{id}/delete")
  public String confirm(@PathVariable String kind, @PathVariable @Positive long id, Model model) {
    var catalog = CatalogKind.parse(kind);
    var row = rows(catalog).stream().filter(v -> v.id() == id).findFirst()
      .orElseThrow(this::missing);
    model.addAttribute("kind", catalog.path());
    model.addAttribute("row", row);
    return "admin/catalog-delete";
  }

  @PostMapping(value = "/admin/catalog/{kind}/{id}/delete", consumes = "application/x-www-form-urlencoded")
  public String delete(@PathVariable String kind, @PathVariable @Positive long id,
    Authentication user, HttpServletRequest request, HttpServletResponse response) {
    var catalog = CatalogKind.parse(kind);
    onlyCsrf(request);
    session.call(user, request, response, token -> {
      admin.delete(token, catalog, id);
      return true;
    });
    return "redirect:/admin/catalog/" + catalog.path();
  }

  private String prepare(CatalogKind kind, Long id, Model model) {
    model.addAttribute("kind", kind.path());
    model.addAttribute("itemId", id);
    if (kind == CatalogKind.DOCTORS) {
      model.addAttribute("hospitals", directory.hospitals());
      model.addAttribute("specialties", directory.specialties());
    }
    return "admin/" + kind.path() + "-form";
  }

  private List<CatalogRow> rows(CatalogKind kind) {
    return switch (kind) {
      case HOSPITALS ->
        directory.hospitals().stream().map(h -> new CatalogRow(h.id(), h.name())).toList();
      case SPECIALTIES ->
        directory.specialties().stream().map(s -> new CatalogRow(s.id(), s.name())).toList();
      case DOCTORS -> directory.doctors(new DoctorSearchInput(null, List.of())).stream()
        .map(d -> new CatalogRow(d.id(), d.firstName() + " " + d.lastName())).toList();
    };
  }

  static void onlyCsrf(HttpServletRequest request) {
    if (request.getParameterMap().keySet().stream().anyMatch(p -> !p.equals("_csrf"))) {
      throw new AdminException(AdminException.Kind.INVALID);
    }
  }

  private AdminException missing() {
    return new AdminException(AdminException.Kind.NOT_FOUND);
  }
}
