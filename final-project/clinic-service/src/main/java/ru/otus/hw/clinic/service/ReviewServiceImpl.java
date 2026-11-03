package ru.otus.hw.clinic.service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.dto.ReviewDto;
import ru.otus.hw.clinic.dto.ReviewInputDto;
import ru.otus.hw.clinic.exception.AppointmentConflictException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.mapper.ReviewMapper;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.AppointmentStatus;
import ru.otus.hw.clinic.model.DoctorAccount;
import ru.otus.hw.clinic.model.Review;
import ru.otus.hw.clinic.repository.AppointmentRepository;
import ru.otus.hw.clinic.repository.DoctorAccountRepository;
import ru.otus.hw.clinic.repository.ReviewRepository;
import ru.otus.hw.clinic.security.CurrentAccount;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;

  private final AppointmentRepository appointmentRepository;

  private final DoctorAccountRepository doctorAccountRepository;

  private final CurrentAccount currentAccount;

  private final ReviewMapper reviewMapper;

  private final Clock clock;

  @Override
  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
  public ReviewDto findByAppointmentId(UUID appointmentId) {
    Review review = reviewRepository.findByAppointmentId(appointmentId).orElseThrow(this::notFound);
    Appointment appointment = review.getAppointment();
    if (currentAccount.hasRole("PATIENT")
      && appointment.getPatient().getAccountId().equals(currentAccount.id())) {
      return reviewMapper.toDto(review);
    }
    if (currentAccount.hasRole("DOCTOR")) {
      Optional<DoctorAccount> doctor = doctorAccountRepository.findById(currentAccount.id());
      if (doctor.isPresent() && doctor.get().getDoctorId() == appointment.getDoctorId()) {
        return reviewMapper.toDto(review);
      }
    }
    throw notFound();
  }

  @Override
  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public ReviewDto create(UUID appointmentId, ReviewInputDto input) {
    Appointment appointment = ownedCompletedAppointment(appointmentId);
    if (reviewRepository.findByAppointmentId(appointmentId).isPresent()) {
      throw new AppointmentConflictException("A review already exists for this appointment");
    }
    Review review = new Review(appointment, input.rating(), input.text(), clock.instant());
    return reviewMapper.toDto(reviewRepository.saveAndFlush(review));
  }

  @Override
  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public ReviewDto update(UUID appointmentId, ReviewInputDto input) {
    ownedCompletedAppointment(appointmentId);
    Review review = reviewRepository.findByAppointmentId(appointmentId).orElseThrow(this::notFound);
    review.update(input.rating(), input.text(), clock.instant());
    reviewRepository.flush();
    return reviewMapper.toDto(review);
  }

  @Override
  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public void delete(UUID appointmentId) {
    ownedCompletedAppointment(appointmentId);
    Review review = reviewRepository.findByAppointmentId(appointmentId).orElseThrow(this::notFound);
    reviewRepository.delete(review);
    reviewRepository.flush();
  }

  private Appointment ownedCompletedAppointment(UUID id) {
    Appointment appointment = appointmentRepository.findLockedById(id).orElseThrow(this::notFound);
    if (!appointment.getPatient().getAccountId().equals(currentAccount.id())) {
      throw notFound();
    }
    if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
      throw new AppointmentConflictException("Only a completed appointment may be reviewed");
    }
    return appointment;
  }

  private ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("Review was not found");
  }
}
