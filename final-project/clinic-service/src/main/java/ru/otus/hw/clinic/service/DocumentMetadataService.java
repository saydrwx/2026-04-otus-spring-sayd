package ru.otus.hw.clinic.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.dto.DocumentDto;
import ru.otus.hw.clinic.dto.DocumentPageDto;
import ru.otus.hw.clinic.exception.AppointmentConflictException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.mapper.DocumentMapper;
import ru.otus.hw.clinic.model.DocumentStatus;
import ru.otus.hw.clinic.model.Patient;
import ru.otus.hw.clinic.model.PatientDocument;
import ru.otus.hw.clinic.repository.PatientDocumentRepository;
import ru.otus.hw.clinic.repository.PatientRepository;
import ru.otus.hw.clinic.security.CurrentAccount;
import ru.otus.hw.clinic.security.DoctorIdentity;
import ru.otus.hw.clinic.security.MedicalRecordAccess;
import ru.otus.hw.clinic.storage.DocumentDescriptor;
import ru.otus.hw.clinic.storage.ValidatedDocument;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class DocumentMetadataService {

  private final PatientDocumentRepository documents;

  private final PatientRepository patients;

  private final DocumentMapper mapper;

  private final CurrentAccount account;

  private final MedicalRecordAccess medicalRecordAccess;

  private final Clock clock;

  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public UUID reserve(ValidatedDocument file) {
    var patient = patients.findByAccountId(account.id())
      .orElseThrow(
        () -> new AppointmentConflictException("Complete your patient profile before uploading"));
    return documents.saveAndFlush(new PatientDocument(patient, file.filename(), file.mediaType(),
      file.bytes().length, file.sha256(), clock.instant())).getId();
  }

  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public DocumentDto activate(UUID id) {
    PatientDocument document = ownedLocked(id);
    document.activate();
    documents.flush();
    return mapper.toDto(document);
  }

  @PreAuthorize("hasRole('PATIENT')")
  @Transactional
  public void requestDelete(UUID id) {
    ownedLocked(id).requestDelete(clock.instant());
    documents.flush();
  }

  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
  public DocumentDescriptor findReady(UUID id, DoctorIdentity doctor) {
    PatientDocument document = documents.findById(id)
      .filter(d -> d.getStatus() == DocumentStatus.READY)
      .orElseThrow(this::notFound);
    medicalRecordAccess.check(document.getPatient(), doctor);
    return new DocumentDescriptor(mapper.toDto(document), document.getSha256());
  }

  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
  public DocumentPageDto list(UUID patientId, int page, int size, DoctorIdentity doctor) {
    Patient patient = patients.findById(patientId).orElseThrow(this::notFound);
    medicalRecordAccess.check(patient, doctor);
    Page<PatientDocument> result = documents.findAllByPatientIdAndStatus(patientId,
      DocumentStatus.READY,
      PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    return new DocumentPageDto(result.getContent().stream().map(mapper::toDto).toList(),
      page, size, result.getTotalElements(), result.getTotalPages());
  }

  @Transactional
  public boolean failIfStaged(UUID id) {
    Optional<PatientDocument> document = documents.findLockedById(id);
    if (document.isEmpty()) {
      return true;
    }
    if (document.get().getStatus() == DocumentStatus.READY) {
      return false;
    }
    document.get().requestDelete(clock.instant());
    documents.flush();
    return true;
  }

  @Transactional
  public void expireStaged(UUID id, Instant cutoff) {
    documents.findLockedById(id)
      .filter(d -> d.getStatus() == DocumentStatus.STAGED && d.getCreatedAt().isBefore(cutoff))
      .ifPresent(d -> d.requestDelete(clock.instant()));
  }

  public List<UUID> staleIds(Instant cutoff) {
    return documents.findAllByStatusAndCreatedAtBefore(DocumentStatus.STAGED, cutoff,
        PageRequest.of(0, 100))
      .stream().map(PatientDocument::getId).toList();
  }

  public List<UUID> pendingIds() {
    return documents.findAllByStatus(DocumentStatus.DELETE_PENDING,
        PageRequest.of(0, 100, Sort.by("createdAt", "id")))
      .stream().map(PatientDocument::getId).toList();
  }

  public boolean isPending(UUID id) {
    return documents.existsByIdAndStatus(id, DocumentStatus.DELETE_PENDING);
  }

  public boolean exists(UUID id) {
    return documents.existsById(id);
  }

  @Transactional
  public void finishDelete(UUID id) {
    documents.deleteIfStatus(id, DocumentStatus.DELETE_PENDING);
  }

  private PatientDocument ownedLocked(UUID id) {
    PatientDocument document = documents.findLockedById(id).orElseThrow(this::notFound);
    if (!document.getPatient().getAccountId().equals(account.id())) {
      throw notFound();
    }
    return document;
  }

  private ResourceNotFoundException notFound() {
    return new ResourceNotFoundException("Document was not found");
  }
}
