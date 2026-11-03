package ru.otus.hw.clinic.dto;

import java.time.OffsetDateTime;

public record SlotDto(OffsetDateTime start, OffsetDateTime end) {

}
