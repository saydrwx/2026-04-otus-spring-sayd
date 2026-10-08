package ru.otus.hw.web.controller;

import java.time.Clock;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.ClinicalException;
import ru.otus.hw.web.security.ClinicalSession;
import ru.otus.hw.web.service.*;
import ru.otus.hw.web.view.AppointmentLabels;

import static ru.otus.hw.web.exception.ClinicalException.Kind.*;
import static ru.otus.hw.web.controller.ClinicalResponses.actionError;

@RequiredArgsConstructor
@Controller
public class ClinicalController {

  private static final int PAGE_SIZE = 20;
  private final ClinicalService clinical;
  private final AppointmentService appointments;
  private final PatientProfileService profiles;
  private final ClinicalSession session;
  private final AppointmentLabels labels;
  private final Clock appointmentClock;

  @InitBinder("reportForm")
  void bind(WebDataBinder binder) {
    FormBindings.only(binder, "appointmentId", "findings", "diagnosis", "recommendations");
  }

  @GetMapping({"/patient/appointments/{id}/report", "/doctor/appointments/{id}/report"})
  public String report(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    // Medical-record access is intentionally delegated directly to the clinical API, not appointment assignment.
    var value = session.call(user, request, response, token -> clinical.report(token, id));
    model.addAttribute("report", value);
    model.addAttribute("doctorView", doctor(request));
    model.addAttribute("labels", labels.forReports(java.util.List.of(value)));
    return "clinical/report";
  }

  @GetMapping("/patient/reports")
  public String ownReports(@RequestParam(defaultValue = "0") @Min(0) int page, Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    return session.call(user, request, response, token -> {
      var profile = profiles.find(token);
      if (profile.isEmpty()) {
        return "redirect:/patient/onboarding";
      }
      return reportsModel(clinical.reports(token, profile.get().id(), page, PAGE_SIZE), false, null,
        model);
    });
  }

  @GetMapping("/doctor/patients/{id}/reports")
  public String patientReports(@PathVariable UUID id,
    @RequestParam(defaultValue = "0") @Min(0) int page, Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    return session.call(user, request, response,
      token -> reportsModel(clinical.reports(token, id, page, PAGE_SIZE), true, id, model));
  }

  @GetMapping("/doctor/appointments/{id}/report/new")
  public String reportForm(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    var value = session.call(user, request, response, token -> appointments.find(token, id));
    eligible(value);
    var form = new ReportForm();
    form.setAppointmentId(id);
    model.addAttribute("reportForm", form);
    model.addAttribute("appointment", value);
    return "clinical/report-form";
  }

  @PostMapping("/doctor/reports/submit")
  public String submit(@Valid @ModelAttribute("reportForm") ReportForm form, BindingResult errors,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    if (errors.getSuppressedFields().length > 0) {
      errors.reject("unsupported", "Форма содержит неподдерживаемые поля.");
    }
    if (errors.hasErrors()) {
      response.setStatus(400);
      return "clinical/report-form";
    }
    try {
      session.call(user, request, response,
        token -> clinical.submitReport(token, form.getAppointmentId(), form.input()));
      return "redirect:/doctor/appointments/" + form.getAppointmentId() + "/report";
    } catch (ClinicalException exception) {
      if (exception.kind() != INVALID && exception.kind() != UNAVAILABLE) {
        return actionError(exception, form.getAppointmentId(), true, response, model,
          "Проверьте запись и текущее заключение перед повторной отправкой.");
      }
      response.setStatus(exception.kind().status().value());
      model.addAttribute("message", exception.kind() == UNAVAILABLE
        ? "Отправка заключения не подтверждена. Проверьте текущее заключение и запись перед повторной попыткой."
        : "Проверьте все поля заключения: каждое обязательно и допускает не более 10 000 символов.");
      return "clinical/report-form";
    }
  }

  @GetMapping("/doctor/appointments/{id}/no-show")
  public String noShowForm(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    var value = session.call(user, request, response, token -> appointments.find(token, id));
    eligible(value);
    model.addAttribute("appointment", value);
    return "clinical/no-show";
  }

  @PostMapping("/doctor/appointments/{id}/no-show")
  public String noShow(@PathVariable UUID id, @RequestParam MultiValueMap<String, String> fields,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    if (fields.keySet().stream().anyMatch(name -> !name.equals("_csrf"))) {
      throw new ClinicalException(INVALID);
    }
    try {
      session.call(user, request, response, token -> {
        clinical.noShow(token, id);
        return Boolean.TRUE;
      });
      return "redirect:/doctor/appointments/" + id;
    } catch (ClinicalException exception) {
      return actionError(exception, id, true, response, model,
        "Неявка не подтверждена. Проверьте запись перед повторной попыткой.");
    }
  }

  private String reportsModel(ReportPageDto value, boolean doctor, UUID patientId, Model model) {
    model.addAttribute("reports", value);
    model.addAttribute("doctorView", doctor);
    model.addAttribute("patientId", patientId);
    model.addAttribute("labels", labels.forReports(value.content()));
    return "clinical/reports";
  }

  private void eligible(AppointmentDto value) {
    if (value.status() != AppointmentStatus.BOOKED || appointmentClock.instant()
      .isBefore(value.end().toInstant())) {
      throw new ClinicalException(CONFLICT);
    }
  }

  private boolean doctor(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/doctor/");
  }

}
