package ru.otus.hw.web.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.AdminException;
import ru.otus.hw.web.exception.AppointmentsException;
import ru.otus.hw.web.security.AdminSession;
import ru.otus.hw.web.service.AdminService;
import ru.otus.hw.web.service.AppointmentService;
import ru.otus.hw.web.view.StatisticsCsv;

@RequiredArgsConstructor
@Controller
public class AdminController {

  private final AdminService admin;

  private final AppointmentService appointments;

  private final AdminSession session;

  private final Clock appointmentClock;

  @GetMapping("/admin")
  public String home() {
    return "admin/home";
  }

  @GetMapping("/admin/appointments")
  public String appointments(@RequestParam(defaultValue = "0") @Min(0) int page,
    Authentication user, HttpServletRequest request, HttpServletResponse response, Model model) {
    model.addAttribute("appointments",
      callAppointments(user, request, response, token -> appointments.list(token, page, 20)));
    return "admin/appointments";
  }

  @GetMapping("/admin/appointments/{id}")
  public String appointment(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    var item = callAppointments(user, request, response, token -> appointments.find(token, id));
    model.addAttribute("appointment", item);
    model.addAttribute("cancellable",
      item.status() == AppointmentStatus.BOOKED && appointmentClock.instant()
        .isBefore(item.start().toInstant()));
    return "admin/appointment";
  }

  @PostMapping(value = "/admin/appointments/{id}/cancel", consumes = "application/x-www-form-urlencoded")
  public String cancel(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response) {
    AdminCatalogController.onlyCsrf(request);
    callAppointments(user, request, response, token -> {
      appointments.cancel(token, id);
      return true;
    });
    return "redirect:/admin/appointments/" + id;
  }

  @GetMapping("/admin/statistics")
  public String statistics(
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @RequestParam(required = false) Long doctorId, @RequestParam(required = false) Long hospitalId,
    Authentication user, HttpServletRequest request, HttpServletResponse response, Model model) {
    var filter = filter(from, to, doctorId, hospitalId);
    model.addAttribute("statistics",
      session.call(user, request, response, token -> admin.statistics(token, filter)));
    model.addAttribute("filter", filter);
    return "admin/statistics";
  }

  @GetMapping("/admin/statistics.csv")
  public ResponseEntity<byte[]> csv(
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @RequestParam(required = false) Long doctorId, @RequestParam(required = false) Long hospitalId,
    Authentication user, HttpServletRequest request, HttpServletResponse response) {
    var filter = filter(from, to, doctorId, hospitalId);
    var statistics = session.call(user, request, response,
      token -> admin.statistics(token, filter));
    return ResponseEntity.ok()
      .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
      .header("Content-Disposition", "attachment; filename=appointment-statistics.csv")
      .header("Content-Security-Policy", "sandbox; default-src 'none'")
      .header("X-Content-Type-Options", "nosniff").header("Cache-Control", "no-store, private")
      .body(StatisticsCsv.render(statistics));
  }

  private StatisticsFilter filter(LocalDate from, LocalDate to, Long doctorId, Long hospitalId) {
    var today = LocalDate.now(appointmentClock);
    boolean initial = from == null && to == null;
    return new StatisticsFilter(initial ? today.minusDays(29) : from,
      initial ? today.plusDays(1) : to, doctorId, hospitalId);
  }

  private <T> T callAppointments(Authentication user, HttpServletRequest request,
    HttpServletResponse response, java.util.function.Function<String, T> action) {
    try {
      return session.call(user, request, response, action);
    } catch (AppointmentsException exception) {
      throw new AdminException(switch (exception.kind()) {
        case INVALID -> AdminException.Kind.INVALID;
        case FORBIDDEN -> AdminException.Kind.FORBIDDEN;
        case NOT_FOUND -> AdminException.Kind.NOT_FOUND;
        case CONFLICT -> AdminException.Kind.CONFLICT;
        default -> AdminException.Kind.UNAVAILABLE;
      });
    }
  }
}
