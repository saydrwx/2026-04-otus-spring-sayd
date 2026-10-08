package ru.otus.hw.directory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctors")
public class Doctor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "first_name", nullable = false, length = 255)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 255)
  private String lastName;

  @Column(name = "education", nullable = true, length = 1000)
  private String education;

  @Column(name = "experience_years", nullable = false)
  private Integer experienceYears;

  @Column(name = "email", nullable = true, length = 255)
  private String email;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "hospital_id", nullable = false)
  private Hospital hospital;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "doctors_specialties",
    joinColumns = @JoinColumn(name = "doctor_id"),
    inverseJoinColumns = @JoinColumn(name = "specialty_id")
  )
  private Set<Specialty> specialties = new LinkedHashSet<>();

  public Doctor(
    String firstName,
    String lastName,
    String education,
    Integer experienceYears,
    String email,
    Hospital hospital,
    Collection<Specialty> specialties
  ) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.education = education;
    this.experienceYears = experienceYears;
    this.email = email;
    this.hospital = hospital;
    this.specialties.addAll(specialties);
  }

  public void replaceSpecialties(Collection<Specialty> specialties) {
    this.specialties.clear();
    this.specialties.addAll(specialties);
  }

  @Override
  public final boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof Doctor doctor)) {
      return false;
    }

    return getId() != null && getId().equals(doctor.getId());
  }

  @Override
  public final int hashCode() {
    return Doctor.class.hashCode();
  }
}
