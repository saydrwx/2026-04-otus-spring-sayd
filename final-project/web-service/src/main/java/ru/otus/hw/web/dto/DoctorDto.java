package ru.otus.hw.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoctorDto(
  long id,
  String firstName,
  String lastName,
  String education,
  int experienceYears,
  String email,
  HospitalDto hospital,
  List<SpecialtyDto> specialties
) {

}
