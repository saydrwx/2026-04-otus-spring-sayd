package ru.otus.hw.clinic.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.configuration.TimeConfiguration;
import ru.otus.hw.clinic.dto.AvailabilityDto;
import ru.otus.hw.clinic.dto.SlotDto;
import ru.otus.hw.clinic.model.AppointmentStatus;
import ru.otus.hw.clinic.repository.AppointmentRepository;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

  private final ScheduleService scheduleService;

  private final AvailabilityCalculator availabilityCalculator;

  private final AppointmentRepository appointmentRepository;

  @Override
  public AvailabilityDto findByDoctorId(long doctorId, LocalDate date) {
    AvailabilityDto candidate = availabilityCalculator.calculate(
      scheduleService.findByDoctorId(doctorId), date);
    if (candidate.slots().isEmpty()) {
      return candidate;
    }
    OffsetDateTime from = date.atStartOfDay().atOffset(TimeConfiguration.HOSPITAL_OFFSET);
    Set<Instant> reserved = appointmentRepository.findReservedStarts(doctorId, from,
        from.plusDays(1), AppointmentStatus.CANCELLED).stream()
      .map(OffsetDateTime::toInstant)
      .collect(Collectors.toSet());
    List<SlotDto> slots = candidate.slots().stream()
      .filter(slot -> !reserved.contains(slot.start().toInstant()))
      .toList();
    return new AvailabilityDto(
      candidate.doctorId(),
      candidate.hospitalId(),
      candidate.date(),
      candidate.timezone(),
      candidate.slotDurationMinutes(),
      candidate.earliestStart(),
      candidate.latestStart(),
      slots
    );
  }
}
