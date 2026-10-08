package ru.otus.hw.clinic.mapper;

import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.configuration.TimeConfiguration;
import ru.otus.hw.clinic.dto.AppointmentDto;
import ru.otus.hw.clinic.dto.AppointmentPageDto;
import ru.otus.hw.clinic.model.Appointment;

@Component
public class AppointmentMapper {

  public AppointmentDto toDto(Appointment appointment) {
    ZoneOffset offset = TimeConfiguration.HOSPITAL_OFFSET;
    return new AppointmentDto(
      appointment.getId(),
      appointment.getPatient().getId(),
      appointment.getDoctorId(),
      appointment.getHospitalId(),
      appointment.getStartAt().withOffsetSameInstant(offset),
      appointment.getEndAt().withOffsetSameInstant(offset),
      appointment.getStatus(),
      appointment.getBookedAt(),
      appointment.getCancelledAt(),
      appointment.getOutcomeAt()
    );
  }

  public AppointmentPageDto toPageDto(Page<Appointment> page) {
    return new AppointmentPageDto(
      page.getContent().stream().map(this::toDto).toList(),
      page.getNumber(),
      page.getSize(),
      page.getTotalElements(),
      page.getTotalPages()
    );
  }
}
