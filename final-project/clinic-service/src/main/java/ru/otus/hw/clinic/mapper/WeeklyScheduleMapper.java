package ru.otus.hw.clinic.mapper;

import java.time.DayOfWeek;
import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.dto.WorkingDayDto;
import ru.otus.hw.clinic.model.WeeklySchedule;

@Component
public class WeeklyScheduleMapper {

  public WorkingDayDto toDto(WeeklySchedule schedule) {
    return new WorkingDayDto(
      DayOfWeek.of(schedule.getDayOfWeek()),
      schedule.getStartTime(),
      schedule.getEndTime()
    );
  }
}
