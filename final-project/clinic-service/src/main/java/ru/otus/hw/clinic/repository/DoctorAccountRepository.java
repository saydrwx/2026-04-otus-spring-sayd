package ru.otus.hw.clinic.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.clinic.model.DoctorAccount;

public interface DoctorAccountRepository extends JpaRepository<DoctorAccount, UUID> { }
