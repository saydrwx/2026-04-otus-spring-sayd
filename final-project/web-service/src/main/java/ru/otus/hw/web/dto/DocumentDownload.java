package ru.otus.hw.web.dto;

public record DocumentDownload(DocumentDto metadata, byte[] bytes) {

  @Override
  public String toString() {
    return "DocumentDownload[documentData=REDACTED]";
  }
}
