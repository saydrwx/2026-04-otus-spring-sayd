package ru.otus.hw.clinic.controller;

import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.clinic.dto.AvailabilityDto;
import ru.otus.hw.clinic.dto.DoctorScheduleDto;
import ru.otus.hw.clinic.service.AvailabilityService;
import ru.otus.hw.clinic.service.ScheduleService;

@RequiredArgsConstructor
@RestController
public class DoctorScheduleController {

  private final ScheduleService scheduleService;

  private final AvailabilityService availabilityService;

  @GetMapping("/api/doctors/{doctorId}/schedule")
  public DoctorScheduleDto findSchedule(@PathVariable @Positive long doctorId) {
    return scheduleService.findByDoctorId(doctorId);
  }

  @GetMapping("/api/doctors/{doctorId}/availability")
  public AvailabilityDto findAvailability(
    @PathVariable @Positive long doctorId,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    return availabilityService.findByDoctorId(doctorId, date);
  }
}
