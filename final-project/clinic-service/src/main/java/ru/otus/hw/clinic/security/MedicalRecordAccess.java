package ru.otus.hw.clinic.security;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.configuration.TimeConfiguration;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.model.AppointmentStatus;
import ru.otus.hw.clinic.model.Patient;
import ru.otus.hw.clinic.repository.AppointmentRepository;

@RequiredArgsConstructor
@Component
public class MedicalRecordAccess {

  private final CurrentAccount currentAccount;

  private final AppointmentRepository appointmentRepository;

  private final Clock clock;

  public void check(Patient patient, DoctorIdentity doctor) {
    if (currentAccount.hasRole("PATIENT") && patient.getAccountId().equals(currentAccount.id())) {
      return;
    }
    if (currentAccount.hasRole("DOCTOR") && doctor != null
      && appointmentRepository.countQualifyingRecordAccess(patient.getId(), doctor.doctorId(),
      doctor.hospitalId(), clock.instant().atOffset(TimeConfiguration.HOSPITAL_OFFSET),
      AppointmentStatus.BOOKED, AppointmentStatus.COMPLETED) > 0) {
      return;
    }
    throw new ResourceNotFoundException("Medical record was not found");
  }
}
