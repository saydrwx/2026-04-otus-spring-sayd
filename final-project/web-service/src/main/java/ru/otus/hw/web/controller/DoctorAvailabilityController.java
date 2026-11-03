package ru.otus.hw.web.controller;

import java.time.Clock;
import java.time.LocalDate;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.service.AppointmentService;
import ru.otus.hw.web.service.DirectoryService;
import ru.otus.hw.web.exception.AppointmentsException;

@RequiredArgsConstructor
@Controller
public class DoctorAvailabilityController {

  private final AppointmentService appointments;

  private final DirectoryService directory;

  private final Clock appointmentClock;

  @GetMapping("/doctors/{id}/availability")
  public String availability(@PathVariable @Positive long id,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
    Model model) {
    var selected = date == null ? LocalDate.now(appointmentClock).plusDays(1) : date;
    var doctor = directory.doctor(id);
    var schedule = appointments.schedule(id);
    var availability = appointments.availability(id, selected);
    if (doctor.hospital().id() != schedule.hospitalId()
      || schedule.hospitalId() != availability.hospitalId()) {
      throw new AppointmentsException(AppointmentsException.Kind.CONFLICT);
    }
    model.addAttribute("doctor", doctor);
    model.addAttribute("schedule", schedule);
    model.addAttribute("availability", availability);
    model.addAttribute("selectedDate", selected);
    return "appointments/availability";
  }
}
