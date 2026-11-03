package ru.otus.hw.clinic.service;

import ru.otus.hw.clinic.dto.PatientDto;
import ru.otus.hw.clinic.dto.PatientInputDto;

public interface PatientService {

  PatientDto findCurrent();

  PatientDto createCurrent(PatientInputDto input);

  PatientDto updateCurrent(PatientInputDto input);
}
