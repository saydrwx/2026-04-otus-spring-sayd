package ru.otus.hw.web.dto;

public record ReportRequest(String findings, String diagnosis, String recommendations) {

  @Override
  public String toString() {
    return "ReportRequest[clinicalData=REDACTED]";
  }
}
