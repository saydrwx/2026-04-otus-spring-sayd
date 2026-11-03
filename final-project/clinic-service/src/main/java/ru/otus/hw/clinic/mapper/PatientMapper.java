package ru.otus.hw.clinic.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.dto.PatientDto;
import ru.otus.hw.clinic.dto.PatientInputDto;
import ru.otus.hw.clinic.model.Patient;

@Component
public class PatientMapper {

  public PatientDto toDto(Patient patient) {
    return new PatientDto(patient.getId(), patient.getFirstName(), patient.getLastName(),
      patient.getMiddleName(), patient.getBirthDate());
  }

  public void updateEntity(Patient patient, PatientInputDto input) {
    patient.setFirstName(input.firstName().strip());
    patient.setLastName(input.lastName().strip());
    String middleName = input.middleName();
    patient.setMiddleName(middleName == null || middleName.isBlank() ? null : middleName.strip());
    patient.setBirthDate(input.birthDate());
  }
}
