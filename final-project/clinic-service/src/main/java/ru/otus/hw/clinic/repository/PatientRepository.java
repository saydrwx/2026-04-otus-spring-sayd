package ru.otus.hw.clinic.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.clinic.model.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

  Optional<Patient> findByAccountId(UUID accountId);

  boolean existsByAccountId(UUID accountId);
}
