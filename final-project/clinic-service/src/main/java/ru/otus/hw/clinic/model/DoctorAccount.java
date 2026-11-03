package ru.otus.hw.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctor_accounts")
public class DoctorAccount {

  @Id
  @Column(name = "account_id", updatable = false)
  private UUID accountId;

  @Column(name = "doctor_id", nullable = false, unique = true, updatable = false)
  private long doctorId;
}
