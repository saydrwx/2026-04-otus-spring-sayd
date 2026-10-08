package ru.otus.hw.authorization.dto;

import java.util.UUID;

public record RegistrationDto(UUID id, String email, String role) {

}
