package ru.otus.hw.authorization.repository;

import java.util.Optional;
import java.util.UUID;
import ru.otus.hw.authorization.model.Account;

public interface AccountRepository {

  Optional<Account> findByEmail(String email);

  Optional<Account> findById(UUID id);

  boolean emailExists(String normalizedEmail);

  boolean insertPatient(UUID id, String normalizedEmail, String encodedPassword);
}
