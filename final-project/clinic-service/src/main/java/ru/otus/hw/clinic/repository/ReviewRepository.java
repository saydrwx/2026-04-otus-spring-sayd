package ru.otus.hw.clinic.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.clinic.model.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

  @EntityGraph(attributePaths = {"appointment", "appointment.patient"})
  Optional<Review> findByAppointmentId(UUID appointmentId);
}
