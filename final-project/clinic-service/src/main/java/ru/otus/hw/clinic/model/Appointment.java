package ru.otus.hw.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.otus.hw.clinic.exception.AppointmentConflictException;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false, updatable = false)
  private Patient patient;

  @Column(name = "doctor_id", nullable = false, updatable = false)
  private long doctorId;

  @Column(name = "hospital_id", nullable = false, updatable = false)
  private long hospitalId;

  @Column(name = "start_at", nullable = false, updatable = false)
  private OffsetDateTime startAt;

  @Column(name = "end_at", nullable = false, updatable = false)
  private OffsetDateTime endAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppointmentStatus status;

  @Column(name = "booked_at", nullable = false, updatable = false)
  private Instant bookedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancelled_by")
  private UUID cancelledBy;

  @Column(name = "outcome_at")
  private Instant outcomeAt;

  @Column(name = "outcome_by")
  private UUID outcomeBy;

  @Version
  private long version;

  public Appointment(Patient patient, long doctorId, long hospitalId,
    OffsetDateTime startAt, OffsetDateTime endAt, Instant bookedAt) {
    this.patient = patient;
    this.doctorId = doctorId;
    this.hospitalId = hospitalId;
    this.startAt = startAt;
    this.endAt = endAt;
    this.bookedAt = bookedAt;
    this.status = AppointmentStatus.BOOKED;
  }

  public void cancel(UUID actor, Instant now) {
    if (status == AppointmentStatus.CANCELLED) {
      return;
    }
    if (status != AppointmentStatus.BOOKED || !now.isBefore(startAt.toInstant())) {
      throw new AppointmentConflictException(
        "Only a booked appointment before its start can be cancelled");
    }
    status = AppointmentStatus.CANCELLED;
    cancelledAt = now;
    cancelledBy = actor;
  }

  public void complete(UUID actor, Instant now) {
    recordOutcome(AppointmentStatus.COMPLETED, actor, now);
  }

  public void markNoShow(UUID actor, Instant now) {
    recordOutcome(AppointmentStatus.NO_SHOW, actor, now);
  }

  private void recordOutcome(AppointmentStatus outcome, UUID actor, Instant now) {
    if (status != AppointmentStatus.BOOKED || now.isBefore(endAt.toInstant())) {
      throw new AppointmentConflictException(
        "Only a booked appointment at or after its end can receive an outcome");
    }
    status = outcome;
    outcomeAt = now;
    outcomeBy = actor;
  }
}
