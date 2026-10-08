package ru.otus.hw.web.controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.AppointmentsException;
import ru.otus.hw.web.security.AppointmentSession;
import ru.otus.hw.web.service.AppointmentService;
import ru.otus.hw.web.service.DirectoryService;
import ru.otus.hw.web.service.PatientProfileService;
import ru.otus.hw.web.view.AppointmentLabels;
import ru.otus.hw.web.view.ClinicTimes;

import static ru.otus.hw.web.exception.AppointmentsException.Kind.*;

@RequiredArgsConstructor
@Controller
public class AppointmentController {

  private static final int PAGE_SIZE = 20;
  private final AppointmentService appointments;
  private final PatientProfileService profiles;
  private final DirectoryService directory;
  private final AppointmentSession session;
  private final AppointmentLabels labels;
  private final Clock appointmentClock;

  @InitBinder("booking")
  void bind(WebDataBinder binder) {
    FormBindings.only(binder, "doctorId", "start");
  }

  @GetMapping({"/patient/appointments", "/doctor/appointments"})
  public String list(@RequestParam(defaultValue = "0") @Min(0) int page, Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    boolean doctor = isDoctor(request);
    model.addAttribute("doctorView", doctor);
    return session.call(user, request, response, token -> {
      if (!doctor && profiles.find(token).isEmpty()) {
        return "redirect:/patient/onboarding";
      }
      try {
        var result = appointments.list(token, page, PAGE_SIZE);
        model.addAttribute("appointments", result);
        model.addAttribute("labels", labels.forAppointments(result.content()));
        return "appointments/list";
      } catch (AppointmentsException exception) {
        if (exception.kind() != CONFLICT) {
          throw exception;
        }
        response.setStatus(409);
        model.addAttribute("message", doctor
          ? "Учётную запись врача необходимо связать со справочником. Обратитесь в клинику."
          : "Чтобы открыть записи на приём, заполните профиль пациента.");
        return "appointments/error";
      }
    });
  }

  @GetMapping({"/patient/appointments/{id}", "/doctor/appointments/{id}"})
  public String detail(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    var value = session.call(user, request, response, token -> appointments.find(token, id));
    detailModel(value, request, model);
    return "appointments/detail";
  }

  @GetMapping("/patient/appointments/new")
  public String confirm(@RequestParam @Positive long doctorId,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    if (session.call(user, request, response, token -> profiles.find(token)).isEmpty()) {
      return "redirect:/patient/onboarding";
    }
    var doctor = directory.doctor(doctorId);
    var date = start.withOffsetSameInstant(ClinicTimes.OFFSET).toLocalDate();
    var availability = appointments.availability(doctorId, date);
    if (availability.hospitalId() != doctor.hospital().id() || availability.slots().stream()
      .noneMatch(s -> s.start().toInstant().equals(start.toInstant()))) {
      return bookingError(CONFLICT, doctorId, start, response, model);
    }
    model.addAttribute("doctor", doctor);
    model.addAttribute("start", start);
    model.addAttribute("end",
      availability.slots().stream().filter(s -> s.start().toInstant().equals(start.toInstant()))
        .findFirst().orElseThrow().end());
    return "appointments/confirm";
  }

  @PostMapping("/patient/appointments/book")
  public String book(@Valid @ModelAttribute("booking") BookingForm form, BindingResult errors,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    if (errors.hasErrors() || errors.getSuppressedFields().length > 0) {
      throw new AppointmentsException(INVALID);
    }
    try {
      return session.call(user, request, response, token -> {
        if (profiles.find(token).isEmpty()) {
          return "redirect:/patient/onboarding";
        }
        var booked = appointments.book(token, form.input());
        return "redirect:/patient/appointments/" + booked.id();
      });
    } catch (AppointmentsException exception) {
      if (exception.kind() != CONFLICT && exception.kind() != INVALID
        && exception.kind() != UNAVAILABLE) {
        throw exception;
      }
      return bookingError(exception.kind(), form.getDoctorId(), form.getStart(), response, model);
    }
  }

  @GetMapping("/patient/appointments/{id}/cancel")
  public String confirmCancel(@PathVariable UUID id, Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    var value = session.call(user, request, response, token -> appointments.find(token, id));
    detailModel(value, request, model);
    return "appointments/cancel";
  }

  @PostMapping("/patient/appointments/{id}/cancel")
  public String cancel(@PathVariable UUID id, @RequestParam MultiValueMap<String, String> fields,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    // Inspect submitted parameters only: MVC binding also adds the legitimate URI variable 'id'.
    if (fields.keySet().stream().anyMatch(name -> !name.equals("_csrf"))) {
      throw new AppointmentsException(INVALID);
    }
    try {
      session.call(user, request, response, token -> {
        appointments.cancel(token, id);
        return Boolean.TRUE;
      });
      return "redirect:/patient/appointments/" + id;
    } catch (AppointmentsException exception) {
      if (exception.kind() != CONFLICT && exception.kind() != UNAVAILABLE) {
        throw exception;
      }
      response.setStatus(exception.kind().status().value());
      model.addAttribute("doctorView", false);
      model.addAttribute("appointmentId", id);
      model.addAttribute("message", exception.kind() == CONFLICT
        ? "Эту запись больше нельзя отменить. Проверьте её текущий статус."
        : "Отмена не подтверждена. Проверьте запись перед повторной отправкой.");
      return "appointments/error";
    }
  }

  private void detailModel(AppointmentDto value, HttpServletRequest request, Model model) {
    boolean doctor = isDoctor(request);
    model.addAttribute("doctorView", doctor);
    model.addAttribute("appointment", value);
    model.addAttribute("labels", labels.forAppointments(List.of(value)));
    model.addAttribute("canRecordOutcome",
      doctor && value.status() == AppointmentStatus.BOOKED && !appointmentClock.instant()
        .isBefore(value.end().toInstant()));
    model.addAttribute("canCancel",
      !doctor && value.status() == AppointmentStatus.BOOKED && appointmentClock.instant()
        .isBefore(value.start().toInstant()));
  }

  private String bookingError(AppointmentsException.Kind kind, long doctorId, OffsetDateTime start,
    HttpServletResponse response, Model model) {
    response.setStatus(kind.status().value());
    model.addAttribute("doctorView", false);
    model.addAttribute("doctorId", doctorId);
    model.addAttribute("selectedDate",
      start.withOffsetSameInstant(ClinicTimes.OFFSET).toLocalDate());
    model.addAttribute("message", kind == CONFLICT
      ? "Это время уже могло быть занято либо нужно проверить профиль. Проверьте свои записи и снова выберите свободное время."
      : kind == INVALID
        ? "Это время вне расписания или текущего периода записи. Снова выберите свободное время."
        : "Запись не подтверждена. Проверьте свои записи перед повторной отправкой.");
    return "appointments/error";
  }

  private boolean isDoctor(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/doctor/");
  }
}
