package ru.otus.hw.clinic.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.clinic.dto.AppointmentDto;
import ru.otus.hw.clinic.dto.AppointmentInputDto;
import ru.otus.hw.clinic.dto.AppointmentPageDto;
import ru.otus.hw.clinic.service.AppointmentService;

@RequiredArgsConstructor
@RestController
public class AppointmentController {

  private final AppointmentService appointmentService;

  @GetMapping("/api/appointments/{id}")
  public AppointmentDto findById(@PathVariable UUID id) {
    return appointmentService.findById(id);
  }

  @GetMapping("/api/appointments")
  public AppointmentPageDto findCurrent(
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return appointmentService.findCurrent(page, size);
  }

  @PostMapping("/api/appointments")
  public ResponseEntity<AppointmentDto> book(@Valid @RequestBody AppointmentInputDto input) {
    AppointmentDto appointment = appointmentService.book(input);
    return ResponseEntity.created(URI.create("/api/appointments/" + appointment.id()))
      .body(appointment);
  }

  @PostMapping("/api/appointments/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable UUID id) {
    appointmentService.cancel(id);
  }
}
