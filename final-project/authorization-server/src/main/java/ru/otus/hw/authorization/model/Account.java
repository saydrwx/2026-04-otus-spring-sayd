package ru.otus.hw.authorization.model;

import java.util.UUID;

public record Account(UUID id, String email, String password, String role) { }
