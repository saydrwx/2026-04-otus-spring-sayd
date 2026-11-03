package ru.otus.hw.clinic.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportInputDto;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.mapper.MedicalReportMapper;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.MedicalReport;
import ru.otus.hw.clinic.repository.AppointmentRepository;
import ru.otus.hw.clinic.repository.MedicalReportRepository;
import ru.otus.hw.clinic.security.CurrentAccount;
import ru.otus.hw.clinic.security.DoctorIdentity;

@RequiredArgsConstructor
@Service
@PreAuthorize("hasRole('DOCTOR')")
@Transactional
public class VisitTransactionService {

  private final AppointmentRepository appointmentRepository;

  private final MedicalReportRepository medicalReportRepository;

  private final MedicalReportMapper medicalReportMapper;

  private final CurrentAccount currentAccount;

  private final Clock clock;

  public ReportDto submitReport(UUID appointmentId, ReportInputDto input, DoctorIdentity doctor) {
    Appointment appointment = assignedAppointment(appointmentId, doctor);
    Instant now = clock.instant();
    appointment.complete(currentAccount.id(), now);
    MedicalReport report = new MedicalReport(appointment, input.findings(), input.diagnosis(),
      input.recommendations(), now, currentAccount.id());
    return medicalReportMapper.toDto(medicalReportRepository.saveAndFlush(report));
  }

  public void markNoShow(UUID appointmentId, DoctorIdentity doctor) {
    Appointment appointment = assignedAppointment(appointmentId, doctor);
    appointment.markNoShow(currentAccount.id(), clock.instant());
    appointmentRepository.flush();
  }

  private Appointment assignedAppointment(UUID id, DoctorIdentity doctor) {
    Appointment appointment = appointmentRepository.findLockedById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found"));
    if (appointment.getDoctorId() != doctor.doctorId()
      || appointment.getHospitalId() != doctor.hospitalId()) {
      throw new ResourceNotFoundException("Appointment was not found");
    }
    return appointment;
  }
}
