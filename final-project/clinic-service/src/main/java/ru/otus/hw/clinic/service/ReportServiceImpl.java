package ru.otus.hw.clinic.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportPageDto;
import ru.otus.hw.clinic.security.CurrentAccount;
import ru.otus.hw.clinic.security.DoctorIdentity;

@RequiredArgsConstructor
@Service
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
public class ReportServiceImpl implements ReportService {

  private final CurrentAccount currentAccount;

  private final DoctorIdentityService doctorIdentityService;

  private final ReportReadService reportReadService;

  @Override
  public ReportDto findByAppointmentId(UUID appointmentId) {
    return reportReadService.findByAppointmentId(appointmentId, doctorIdentity());
  }

  @Override
  public ReportPageDto findForPatient(UUID patientId, int page, int size) {
    return reportReadService.findForPatient(patientId, page, size, doctorIdentity());
  }

  private DoctorIdentity doctorIdentity() {
    return currentAccount.hasRole("DOCTOR") ? doctorIdentityService.current() : null;
  }
}
