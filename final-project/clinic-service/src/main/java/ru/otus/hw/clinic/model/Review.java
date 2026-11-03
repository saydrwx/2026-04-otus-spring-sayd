package ru.otus.hw.clinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reviews")
public class Review {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "appointment_id", nullable = false, unique = true, updatable = false)
  private Appointment appointment;

  @Column(nullable = false)
  private int rating;

  @Column(length = 2000)
  private String text;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private long version;

  public Review(Appointment appointment, int rating, String text, Instant now) {
    this.appointment = appointment;
    this.createdAt = now;
    update(rating, text, now);
  }

  public void update(int rating, String text, Instant now) {
    this.rating = rating;
    this.text = text == null || text.isBlank() ? null : text.strip();
    this.updatedAt = now;
  }
}
