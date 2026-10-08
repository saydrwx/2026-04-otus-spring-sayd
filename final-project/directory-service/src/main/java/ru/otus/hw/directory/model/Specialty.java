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
@Table(name = "specialties")
public class Specialty {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  public Specialty(String name) {
    this.name = name;
  }

  @Override
  public final boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof Specialty specialty)) {
      return false;
    }

    return getId() != null && getId().equals(specialty.getId());
  }

  @Override
  public final int hashCode() {
    return Specialty.class.hashCode();
  }
}
