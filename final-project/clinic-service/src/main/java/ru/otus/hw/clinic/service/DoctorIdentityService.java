package ru.otus.hw.clinic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ru.otus.hw.clinic.client.DirectoryClient;
import ru.otus.hw.clinic.client.DirectoryDoctorDto;
import ru.otus.hw.clinic.exception.AppointmentConflictException;
import ru.otus.hw.clinic.model.DoctorAccount;
import ru.otus.hw.clinic.repository.DoctorAccountRepository;
import ru.otus.hw.clinic.security.CurrentAccount;
import ru.otus.hw.clinic.security.DoctorIdentity;

@RequiredArgsConstructor
@Service
public class DoctorIdentityService {

  private final CurrentAccount currentAccount;

  private final DoctorAccountRepository doctorAccountRepository;

  private final DirectoryClient directoryClient;

  @PreAuthorize("hasRole('DOCTOR')")
  public DoctorIdentity current() {
    DoctorAccount mapping = doctorAccountRepository.findById(currentAccount.id())
      .orElseThrow(
        () -> new AppointmentConflictException("Doctor account link has not been configured"));
    DirectoryDoctorDto doctor = directoryClient.findDoctor(mapping.getDoctorId());
    return new DoctorIdentity(doctor.id(), doctor.hospital().id());
  }
}
