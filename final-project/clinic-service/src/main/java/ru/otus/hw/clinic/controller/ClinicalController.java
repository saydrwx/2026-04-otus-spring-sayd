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
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportInputDto;
import ru.otus.hw.clinic.dto.ReportPageDto;
import ru.otus.hw.clinic.service.ReportService;
import ru.otus.hw.clinic.service.VisitService;

@RequiredArgsConstructor
@RestController
public class ClinicalController {

  private final VisitService visitService;

  private final ReportService reportService;

  @PostMapping("/api/appointments/{id}/report")
  public ResponseEntity<ReportDto> submitReport(@PathVariable UUID id,
    @Valid @RequestBody ReportInputDto input) {
    return ResponseEntity.created(URI.create("/api/appointments/" + id + "/report"))
      .body(visitService.submitReport(id, input));
  }

  @PostMapping("/api/appointments/{id}/no-show")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markNoShow(@PathVariable UUID id) {
    visitService.markNoShow(id);
  }

  @GetMapping("/api/appointments/{id}/report")
  public ReportDto findReport(@PathVariable UUID id) {
    return reportService.findByAppointmentId(id);
  }

  @GetMapping("/api/patients/{patientId}/reports")
  public ReportPageDto findReports(@PathVariable UUID patientId,
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return reportService.findForPatient(patientId, page, size);
  }
}
