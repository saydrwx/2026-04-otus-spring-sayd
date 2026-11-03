package ru.otus.hw.clinic.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.configuration.TimeConfiguration;
import ru.otus.hw.clinic.dto.AvailabilityDto;
import ru.otus.hw.clinic.dto.DoctorScheduleDto;
import ru.otus.hw.clinic.dto.SlotDto;

@Component
public class AvailabilityCalculator {

  private final Clock clock;

  public AvailabilityCalculator(Clock clock) {
    this.clock = clock;
  }

  public AvailabilityDto calculate(DoctorScheduleDto schedule, LocalDate date) {
    var now = clock.instant();
    var offset = TimeConfiguration.HOSPITAL_OFFSET;
    var earliest = now.atOffset(offset).toLocalDate().plusDays(1)
      .atStartOfDay().atOffset(offset).toInstant();
    var latest = now.plus(Duration.ofDays(14));
    var slots = new ArrayList<SlotDto>();
    for (var workingDay : schedule.workingDays()) {
      if (workingDay.dayOfWeek() != date.getDayOfWeek()) {
        continue;
      }
      var shiftEnd = date.atTime(workingDay.endTime()).atOffset(offset);
      for (var start = date.atTime(workingDay.startTime()).atOffset(offset);
        !start.plus(TimeConfiguration.SLOT_DURATION).isAfter(shiftEnd);
        start = start.plus(TimeConfiguration.SLOT_DURATION)) {
        var instant = start.toInstant();
        if (!instant.isBefore(earliest) && !instant.isAfter(latest)) {
          slots.add(new SlotDto(start, start.plus(TimeConfiguration.SLOT_DURATION)));
        }
      }
    }
    return new AvailabilityDto(schedule.doctorId(), schedule.hospitalId(), date, offset.getId(),
      (int) TimeConfiguration.SLOT_DURATION.toMinutes(), OffsetDateTime.ofInstant(earliest, offset),
      OffsetDateTime.ofInstant(latest, offset), java.util.List.copyOf(slots));
  }
}
