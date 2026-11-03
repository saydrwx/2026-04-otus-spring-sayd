package ru.otus.hw.clinic.repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.model.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

  Page<Appointment> findAllByPatientId(UUID patientId, Pageable pageable);

  Page<Appointment> findAllByDoctorId(long doctorId, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Appointment a where a.id = :id")
  Optional<Appointment> findLockedById(@Param("id") UUID id);

  @Query("""
    select a.startAt from Appointment a
    where a.doctorId = :doctorId and a.status <> :cancelled
      and a.startAt >= :from and a.startAt < :to
    """)
  List<OffsetDateTime> findReservedStarts(
    @Param("doctorId") long doctorId,
    @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
    @Param("cancelled") AppointmentStatus cancelled
  );

  @Query("""
    select count(a) from Appointment a
    where a.patient.id = :patientId and a.doctorId = :doctorId and a.hospitalId = :hospitalId
      and (a.status = :completed or (a.status = :booked and a.startAt > :now))
    """)
  long countQualifyingRecordAccess(
    @Param("patientId") UUID patientId,
    @Param("doctorId") long doctorId,
    @Param("hospitalId") long hospitalId,
    @Param("now") OffsetDateTime now,
    @Param("booked") AppointmentStatus booked,
    @Param("completed") AppointmentStatus completed
  );
}
