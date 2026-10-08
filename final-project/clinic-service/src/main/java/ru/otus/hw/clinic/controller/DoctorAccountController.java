package ru.otus.hw.clinic.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.clinic.dto.DoctorAccountDto;
import ru.otus.hw.clinic.service.DoctorAccountService;

@RequiredArgsConstructor
@RestController
public class DoctorAccountController {

  private final DoctorAccountService doctorAccountService;

  @GetMapping("/api/doctors/me")
  public DoctorAccountDto findCurrent() {
    return doctorAccountService.findCurrent();
  }
}
