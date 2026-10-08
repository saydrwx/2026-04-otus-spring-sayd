package ru.otus.hw.clinic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;

public record AppointmentInputDto(@Positive long doctorId, @NotNull OffsetDateTime start) {

}
