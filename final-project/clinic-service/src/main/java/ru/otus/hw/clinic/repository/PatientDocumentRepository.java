package ru.otus.hw.clinic.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.otus.hw.clinic.model.DocumentStatus;
import ru.otus.hw.clinic.model.PatientDocument;

public interface PatientDocumentRepository extends JpaRepository<PatientDocument, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from PatientDocument d where d.id = :id")
  Optional<PatientDocument> findLockedById(@Param("id") UUID id);

  Page<PatientDocument> findAllByPatientIdAndStatus(
    UUID patientId,
    DocumentStatus status,
    Pageable pageable
  );

  List<PatientDocument> findAllByStatusAndCreatedAtBefore(
    DocumentStatus status,
    Instant cutoff,
    Pageable pageable
  );

  List<PatientDocument> findAllByStatus(DocumentStatus status, Pageable pageable);

  boolean existsByIdAndStatus(UUID id, DocumentStatus status);

  @Modifying
  @Query("delete from PatientDocument d where d.id = :id and d.status = :status")
  void deleteIfStatus(@Param("id") UUID id, @Param("status") DocumentStatus status);
}
