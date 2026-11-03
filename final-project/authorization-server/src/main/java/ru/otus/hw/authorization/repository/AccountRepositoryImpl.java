package ru.otus.hw.authorization.repository;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.otus.hw.authorization.model.Account;

@RequiredArgsConstructor
@Repository
public class AccountRepositoryImpl implements AccountRepository {

  private final JdbcClient jdbc;

  @Override
  public Optional<Account> findByEmail(String email) {
    return jdbc.sql(
        "SELECT id, email, password, role FROM accounts WHERE lower(btrim(email) COLLATE \"C\") = :email")
      .param("email", email).query(Account.class).optional();
  }

  @Override
  public Optional<Account> findById(UUID id) {
    return jdbc.sql("SELECT id, email, password, role FROM accounts WHERE id = :id")
      .param("id", id).query(Account.class).optional();
  }

  @Override
  public boolean emailExists(String normalizedEmail) {
    return jdbc.sql("""
      SELECT EXISTS (SELECT 1 FROM accounts WHERE lower(btrim(email) COLLATE "C") = :email)
      """).param("email", normalizedEmail).query(Boolean.class).single();
  }

  @Override
  public boolean insertPatient(UUID id, String normalizedEmail, String encodedPassword) {
    return jdbc.sql("""
        INSERT INTO accounts(id,email,password,role) VALUES (:id,:email,:password,'PATIENT')
        ON CONFLICT ((lower(btrim(email) COLLATE "C"))) DO NOTHING
        """).param("id", id).param("email", normalizedEmail).param("password", encodedPassword)
      .update() == 1;
  }
}
