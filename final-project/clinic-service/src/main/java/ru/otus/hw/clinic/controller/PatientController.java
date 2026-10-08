package ru.otus.hw.clinic.controller;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.clinic.dto.PatientDto;
import ru.otus.hw.clinic.dto.PatientInputDto;
import ru.otus.hw.clinic.service.PatientService;

@RequiredArgsConstructor
@RestController
public class PatientController {

  private final PatientService patientService;

  @GetMapping("/api/patients/me")
  public PatientDto findCurrent() {
    return patientService.findCurrent();
  }

  @PostMapping("/api/patients/me")
  public ResponseEntity<PatientDto> createCurrent(@Valid @RequestBody PatientInputDto input) {
    return ResponseEntity.created(URI.create("/api/patients/me"))
      .body(patientService.createCurrent(input));
  }

  @PutMapping("/api/patients/me")
  public PatientDto updateCurrent(@Valid @RequestBody PatientInputDto input) {
    return patientService.updateCurrent(input);
  }
}
