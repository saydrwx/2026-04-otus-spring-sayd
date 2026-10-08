package ru.otus.hw.directory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "hospitals")
public class Hospital {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "address", nullable = false, length = 300)
  private String address;

  public Hospital(String name, String address) {
    this.name = name;
    this.address = address;
  }

  @Override
  public final boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof Hospital hospital)) {
      return false;
    }

    return getId() != null && getId().equals(hospital.getId());
  }

  @Override
  public final int hashCode() {
    return Hospital.class.hashCode();
  }
}
