package ru.otus.hw.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patients")
public class Patient {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_id", nullable = false, unique = true, updatable = false)
  private UUID accountId;

  @Setter
  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Setter
  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Setter
  @Column(name = "middle_name", length = 100)
  private String middleName;

  @Setter
  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  @Version
  private long version;

  public Patient(UUID accountId) {
    this.accountId = accountId;
  }
}
