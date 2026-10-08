package ru.otus.hw.clinic.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.client.DirectoryClient;
import ru.otus.hw.clinic.client.DirectoryDoctorDto;
import ru.otus.hw.clinic.configuration.TimeConfiguration;
import ru.otus.hw.clinic.dto.DoctorScheduleDto;
import ru.otus.hw.clinic.dto.WorkingDayDto;
import ru.otus.hw.clinic.mapper.WeeklyScheduleMapper;
import ru.otus.hw.clinic.repository.WeeklyScheduleRepository;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

  private final DirectoryClient directoryClient;

  private final WeeklyScheduleRepository weeklyScheduleRepository;

  private final WeeklyScheduleMapper weeklyScheduleMapper;

  @Override
  public DoctorScheduleDto findByDoctorId(long doctorId) {
    DirectoryDoctorDto doctor = directoryClient.findDoctor(doctorId);
    List<WorkingDayDto> workingDays = weeklyScheduleRepository.findAllByDoctorIdOrderByDayOfWeekAsc(
        doctorId)
      .stream().map(weeklyScheduleMapper::toDto).toList();
    return new DoctorScheduleDto(
      doctorId,
      doctor.hospital().id(),
      TimeConfiguration.HOSPITAL_OFFSET.getId(),
      (int) TimeConfiguration.SLOT_DURATION.toMinutes(),
      workingDays
    );
  }
}
