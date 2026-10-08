package ru.otus.hw.clinic.storage;

import ru.otus.hw.clinic.dto.DocumentDto;

public record DocumentDescriptor(DocumentDto metadata, String sha256) {

}
