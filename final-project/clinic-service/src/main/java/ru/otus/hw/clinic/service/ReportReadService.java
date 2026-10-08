package ru.otus.hw.clinic.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportPageDto;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.mapper.MedicalReportMapper;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.MedicalReport;
import ru.otus.hw.clinic.model.Patient;
import ru.otus.hw.clinic.repository.MedicalReportRepository;
import ru.otus.hw.clinic.repository.PatientRepository;
import ru.otus.hw.clinic.security.DoctorIdentity;
import ru.otus.hw.clinic.security.MedicalRecordAccess;

@RequiredArgsConstructor
@Service
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
@Transactional(readOnly = true)
public class ReportReadService {

  private final MedicalReportRepository medicalReportRepository;

  private final PatientRepository patientRepository;

  private final MedicalReportMapper medicalReportMapper;

  private final MedicalRecordAccess medicalRecordAccess;

  public ReportDto findByAppointmentId(UUID appointmentId, DoctorIdentity doctor) {
    MedicalReport report = medicalReportRepository.findByAppointmentId(appointmentId)
      .orElseThrow(this::notFound);
    Appointment appointment = report.getAppointment();
    medicalRecordAccess.check(appointment.getPatient(), doctor);
    if (doctor != null && appointment.getHospitalId() != doctor.hospitalId()) {
      throw notFound();
    }
    return medicalReportMapper.toDto(report);
  }

  public ReportPageDto findForPatient(UUID patientId, int page, int size, DoctorIdentity doctor) {
    Patient patient = patientRepository.findById(patientId).orElseThrow(this::notFound);
    medicalRecordAccess.check(patient, doctor);
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt", "id"));
    Page<MedicalReport> reports = doctor == null
      ? medicalReportRepository.findAllByAppointmentPatientId(patientId, pageable)
      : medicalReportRepository.findAllByAppointmentPatientIdAndAppointmentHospitalId(
        patientId, doctor.hospitalId(), pageable);
    return medicalReportMapper.toPageDto(reports);
  }

  private ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("Medical record was not found");
  }
}
