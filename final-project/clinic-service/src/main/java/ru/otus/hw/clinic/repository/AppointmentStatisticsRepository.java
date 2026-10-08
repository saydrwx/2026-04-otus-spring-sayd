package ru.otus.hw.clinic.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.otus.hw.clinic.model.Appointment;
import ru.otus.hw.clinic.dto.AppointmentStatisticRow;

public interface AppointmentStatisticsRepository extends Repository<Appointment, UUID> {

  @Query("""
    select new ru.otus.hw.clinic.dto.AppointmentStatisticRow(a.doctorId,a.hospitalId,a.status,count(a))
    from Appointment a where a.startAt >= :from and a.startAt < :to
      and (:doctorId is null or a.doctorId = :doctorId)
      and (:hospitalId is null or a.hospitalId = :hospitalId)
    group by a.doctorId,a.hospitalId,a.status order by a.doctorId,a.hospitalId,a.status
    """)
  List<AppointmentStatisticRow> aggregate(
    @Param("from") OffsetDateTime from,
    @Param("to") OffsetDateTime to,
    @Param("doctorId") Long doctorId,
    @Param("hospitalId") Long hospitalId,
    Pageable limit
  );
}
