package ru.otus.hw.clinic.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryDoctorDto(long id, Hospital hospital) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Hospital(long id) {

  }
}
