package ru.otus.hw.clinic.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.client.DirectoryDoctorDto;
import ru.otus.hw.clinic.configuration.TimeConfiguration;
import ru.otus.hw.clinic.dto.AppointmentDto;
import ru.otus.hw.clinic.dto.AppointmentInputDto;
import ru.otus.hw.clinic.dto.DoctorScheduleDto;
import ru.otus.hw.clinic.dto.WorkingDayDto;
import ru.otus.hw.clinic.exception.AppointmentConflictException;
import ru.otus.hw.clinic.exception.InvalidBookingException;
import ru.otus.hw.clinic.mapper.AppointmentMapper;
import ru.otus.hw.clinic.mapper.WeeklyScheduleMapper;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.Patient;
import ru.otus.hw.clinic.repository.AppointmentRepository;
import ru.otus.hw.clinic.repository.PatientRepository;
import ru.otus.hw.clinic.repository.WeeklyScheduleRepository;
import ru.otus.hw.clinic.security.CurrentAccount;

@RequiredArgsConstructor
@Service
public class AppointmentBookingService {

  private final PatientRepository patientRepository;

  private final WeeklyScheduleRepository weeklyScheduleRepository;

  private final WeeklyScheduleMapper weeklyScheduleMapper;

  private final AvailabilityCalculator availabilityCalculator;

  private final AppointmentRepository appointmentRepository;

  private final AppointmentMapper appointmentMapper;

  private final CurrentAccount currentAccount;

  private final Clock clock;

  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public AppointmentDto book(AppointmentInputDto input, DirectoryDoctorDto doctor) {
    Patient patient = findCurrentPatient();
    OffsetDateTime start = input.start()
      .withOffsetSameInstant(TimeConfiguration.HOSPITAL_OFFSET);

    DoctorScheduleDto schedule = buildSchedule(doctor);
    validateAvailableStart(schedule, start);

    Appointment appointment = createAppointment(patient, doctor, start);
    return saveAppointment(appointment);
  }

  private Patient findCurrentPatient() {
    return patientRepository.findByAccountId(currentAccount.id())
      .orElseThrow(() -> new AppointmentConflictException(
        "Complete your patient profile before booking"));
  }

  private DoctorScheduleDto buildSchedule(DirectoryDoctorDto doctor) {
    List<WorkingDayDto> workingDays = weeklyScheduleRepository
      .findAllByDoctorIdOrderByDayOfWeekAsc(doctor.id())
      .stream()
      .map(weeklyScheduleMapper::toDto)
      .toList();

    return new DoctorScheduleDto(
      doctor.id(),
      doctor.hospital().id(),
      TimeConfiguration.HOSPITAL_OFFSET.getId(),
      (int) TimeConfiguration.SLOT_DURATION.toMinutes(),
      workingDays
    );
  }

  private void validateAvailableStart(
    DoctorScheduleDto schedule,
    OffsetDateTime start
  ) {
    boolean available = availabilityCalculator
      .calculate(schedule, start.toLocalDate())
      .slots()
      .stream()
      .anyMatch(slot -> slot.start().toInstant().equals(start.toInstant()));

    if (!available) {
      throw new InvalidBookingException(
        "Start must be a scheduled half-hour slot within the booking window");
    }
  }

  private Appointment createAppointment(
    Patient patient,
    DirectoryDoctorDto doctor,
    OffsetDateTime start
  ) {
    return new Appointment(
      patient,
      doctor.id(),
      doctor.hospital().id(),
      start,
      start.plus(TimeConfiguration.SLOT_DURATION),
      clock.instant()
    );
  }

  private AppointmentDto saveAppointment(Appointment appointment) {
    try {
      return appointmentMapper.toDto(
        appointmentRepository.saveAndFlush(appointment));
    } catch (DataIntegrityViolationException exception) {
      if (isReservedSlotViolation(exception)) {
        throw new AppointmentConflictException(
          "The doctor's slot is already reserved");
      }
      throw exception;
    }
  }

  private boolean isReservedSlotViolation(Throwable exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException constraint
        && "uk_appointments_reserved_slot".equals(constraint.getConstraintName())) {
        return true;
      }
    }
    return false;
  }
}
