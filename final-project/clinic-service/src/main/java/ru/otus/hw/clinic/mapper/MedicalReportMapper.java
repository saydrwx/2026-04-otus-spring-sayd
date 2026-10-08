package ru.otus.hw.clinic.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportPageDto;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.MedicalReport;

@Component
public class MedicalReportMapper {

  public ReportDto toDto(MedicalReport report) {
    Appointment appointment = report.getAppointment();
    return new ReportDto(
      report.getId(),
      appointment.getId(),
      appointment.getPatient().getId(),
      appointment.getDoctorId(),
      appointment.getHospitalId(),
      report.getFindings(),
      report.getDiagnosis(),
      report.getRecommendations(),
      report.getSubmittedAt()
    );
  }

  public ReportPageDto toPageDto(Page<MedicalReport> reports) {
    return new ReportPageDto(
      reports.getContent().stream().map(this::toDto).toList(),
      reports.getNumber(),
      reports.getSize(),
      reports.getTotalElements(),
      reports.getTotalPages()
    );
  }
}
