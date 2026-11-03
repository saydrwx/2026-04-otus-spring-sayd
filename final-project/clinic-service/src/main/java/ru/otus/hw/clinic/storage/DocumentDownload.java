package ru.otus.hw.clinic.storage;

import ru.otus.hw.clinic.dto.DocumentDto;

public record DocumentDownload(DocumentDto metadata, byte[] bytes) {

}
