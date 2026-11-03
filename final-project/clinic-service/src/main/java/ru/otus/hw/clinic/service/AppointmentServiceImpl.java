package ru.otus.hw.clinic.service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.client.DirectoryClient;
import ru.otus.hw.clinic.client.DirectoryDoctorDto;
import ru.otus.hw.clinic.dto.AppointmentDto;
import ru.otus.hw.clinic.dto.AppointmentInputDto;
import ru.otus.hw.clinic.dto.AppointmentPageDto;
import ru.otus.hw.clinic.exception.AppointmentConflictException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.mapper.AppointmentMapper;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.DoctorAccount;
import ru.otus.hw.clinic.model.Patient;
import ru.otus.hw.clinic.repository.AppointmentRepository;
import ru.otus.hw.clinic.repository.DoctorAccountRepository;
import ru.otus.hw.clinic.repository.PatientRepository;
import ru.otus.hw.clinic.security.CurrentAccount;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

  private final DirectoryClient directoryClient;

  private final AppointmentBookingService appointmentBookingService;

  private final AppointmentRepository appointmentRepository;

  private final PatientRepository patientRepository;

  private final DoctorAccountRepository doctorAccountRepository;

  private final AppointmentMapper appointmentMapper;

  private final CurrentAccount currentAccount;

  private final Clock clock;

  @Override
  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public AppointmentDto book(AppointmentInputDto input) {
    DirectoryDoctorDto doctor = directoryClient.findDoctor(input.doctorId());
    return appointmentBookingService.book(input, doctor);
  }

  @Override
  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
  public AppointmentDto findById(UUID id) {
    Appointment appointment = appointmentRepository.findById(id).orElseThrow(this::notFound);
    checkAccess(appointment);
    return appointmentMapper.toDto(appointment);
  }

  @Override
  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
  public AppointmentPageDto findCurrent(int page, int size) {
    PageRequest pageable = PageRequest.of(
      page,
      size,
      Sort.by("startAt", "id")
    );
    Page<Appointment> appointments;
    if (currentAccount.hasRole("ADMIN")) {
      appointments = appointmentRepository.findAll(pageable);
    } else if (currentAccount.hasRole("DOCTOR")) {
      long doctorId = doctorAccountRepository.findById(currentAccount.id())
        .orElseThrow(
          () -> new AppointmentConflictException("Doctor account link has not been configured"))
        .getDoctorId();
      appointments = appointmentRepository.findAllByDoctorId(doctorId, pageable);
    } else {
      Patient patient = patientRepository.findByAccountId(currentAccount.id())
        .orElseThrow(() -> new AppointmentConflictException("Complete your patient profile first"));
      appointments = appointmentRepository.findAllByPatientId(patient.getId(), pageable);
    }
    return appointmentMapper.toPageDto(appointments);
  }

  @Override
  @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
  @Transactional
  public void cancel(UUID id) {
    Appointment appointment = appointmentRepository.findLockedById(id).orElseThrow(this::notFound);
    checkAccess(appointment);
    appointment.cancel(currentAccount.id(), clock.instant());
    appointmentRepository.flush();
  }

  private void checkAccess(Appointment appointment) {
    if (currentAccount.hasRole("ADMIN")) {
      return;
    }
    if (currentAccount.hasRole("PATIENT")
      && appointment.getPatient().getAccountId().equals(currentAccount.id())) {
      return;
    }
    if (currentAccount.hasRole("DOCTOR")) {
      Optional<DoctorAccount> mapping = doctorAccountRepository.findById(currentAccount.id());
      if (mapping.isPresent() && mapping.get().getDoctorId() == appointment.getDoctorId()) {
        return;
      }
    }
    throw notFound();
  }

  private ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("Appointment was not found");
  }
}
