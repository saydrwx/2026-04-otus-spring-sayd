package ru.otus.hw.clinic.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.clinic.model.WeeklySchedule;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long> {

  List<WeeklySchedule> findAllByDoctorIdOrderByDayOfWeekAsc(long doctorId);
}
