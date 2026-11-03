package ru.otus.hw.clinic.dto;

import java.util.UUID;

public record DoctorAccountDto(UUID accountId, long doctorId) { }
