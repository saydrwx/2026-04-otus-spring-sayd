package ru.otus.hw.clinic.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.clinic.model.MedicalReport;

public interface MedicalReportRepository extends JpaRepository<MedicalReport, UUID> {

  @EntityGraph(attributePaths = {"appointment", "appointment.patient"})
  Optional<MedicalReport> findByAppointmentId(UUID appointmentId);

  @EntityGraph(attributePaths = {"appointment", "appointment.patient"})
  Page<MedicalReport> findAllByAppointmentPatientId(UUID patientId, Pageable pageable);

  @EntityGraph(attributePaths = {"appointment", "appointment.patient"})
  Page<MedicalReport> findAllByAppointmentPatientIdAndAppointmentHospitalId(
    UUID patientId, long hospitalId, Pageable pageable);
}
