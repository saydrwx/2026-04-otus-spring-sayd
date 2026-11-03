package ru.otus.hw.clinic.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingDayDto(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {

}
