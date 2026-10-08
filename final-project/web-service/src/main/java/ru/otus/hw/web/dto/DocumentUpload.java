package ru.otus.hw.web.dto;

public record DocumentUpload(String filename, String mediaType, byte[] bytes) {

  @Override
  public String toString() {
    return "DocumentUpload[documentData=REDACTED]";
  }
}
