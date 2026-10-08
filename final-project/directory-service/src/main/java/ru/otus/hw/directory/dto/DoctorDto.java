package ru.otus.hw.directory.dto;

import java.util.List;

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
