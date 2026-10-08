package ru.otus.hw.clinic.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportInputDto;

@RequiredArgsConstructor
@Service
@PreAuthorize("hasRole('DOCTOR')")
public class VisitServiceImpl implements VisitService {

  private final DoctorIdentityService doctorIdentityService;

  private final VisitTransactionService visitTransactionService;

  @Override
  public ReportDto submitReport(UUID appointmentId, ReportInputDto input) {
    return visitTransactionService.submitReport(appointmentId, input,
      doctorIdentityService.current());
  }

  @Override
  public void markNoShow(UUID appointmentId) {
    visitTransactionService.markNoShow(appointmentId, doctorIdentityService.current());
  }
}
