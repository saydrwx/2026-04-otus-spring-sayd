package ru.otus.hw.clinic.storage;

public record ValidatedDocument(String filename, String mediaType, byte[] bytes, String sha256) { }
