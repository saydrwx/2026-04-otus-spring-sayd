package ru.otus.hw.web.dto;

public record ReviewRequest(int rating, String text) {

  @Override
  public String toString() {
    return "ReviewRequest[clinicalData=REDACTED]";
  }
}
