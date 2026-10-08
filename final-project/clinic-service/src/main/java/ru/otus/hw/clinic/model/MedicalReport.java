package ru.otus.hw.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@NoArgsConstructor
@Immutable
@Entity
@Table(name = "medical_reports")
public class MedicalReport {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "appointment_id", nullable = false, unique = true, updatable = false)
  private Appointment appointment;

  @Column(nullable = false, length = 10000, updatable = false)
  private String findings;

  @Column(nullable = false, length = 10000, updatable = false)
  private String diagnosis;

  @Column(nullable = false, length = 10000, updatable = false)
  private String recommendations;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  @Column(name = "submitted_by", nullable = false, updatable = false)
  private UUID submittedBy;

  public MedicalReport(
    Appointment appointment,
    String findings,
    String diagnosis,
    String recommendations,
    Instant submittedAt,
    UUID submittedBy
  ) {
    this.appointment = appointment;
    this.findings = findings.strip();
    this.diagnosis = diagnosis.strip();
    this.recommendations = recommendations.strip();
    this.submittedAt = submittedAt;
    this.submittedBy = submittedBy;
  }
}
