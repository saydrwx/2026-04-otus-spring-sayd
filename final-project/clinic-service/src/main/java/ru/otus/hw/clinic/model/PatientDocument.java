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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.otus.hw.clinic.exception.AppointmentConflictException;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient_documents")
public class PatientDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false, updatable = false)
  private Patient patient;

  @Column(nullable = false, length = 200, updatable = false)
  private String filename;

  @Column(name = "media_type", nullable = false, length = 100, updatable = false)
  private String mediaType;

  @Column(name = "size_bytes", nullable = false, updatable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DocumentStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "delete_requested_at")
  private Instant deleteRequestedAt;

  @Version
  private long version;

  public PatientDocument(
    Patient patient,
    String filename,
    String mediaType,
    long sizeBytes,
    String sha256,
    Instant now
  ) {
    this.patient = patient;
    this.filename = filename;
    this.mediaType = mediaType;
    this.sizeBytes = sizeBytes;
    this.sha256 = sha256;
    this.createdAt = now;
    this.status = DocumentStatus.STAGED;
  }

  public void activate() {
    if (status != DocumentStatus.STAGED) {
      throw new AppointmentConflictException("Upload is no longer active");
    }
    status = DocumentStatus.READY;
  }

  public void requestDelete(Instant now) {
    if (status != DocumentStatus.DELETE_PENDING) {
      status = DocumentStatus.DELETE_PENDING;
      deleteRequestedAt = now;
    }
  }
}
