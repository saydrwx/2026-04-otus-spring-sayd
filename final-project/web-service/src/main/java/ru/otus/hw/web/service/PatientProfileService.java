package ru.otus.hw.web.service;

import java.util.Optional;
import ru.otus.hw.web.dto.PatientDto;
import ru.otus.hw.web.dto.PatientInputDto;

public interface PatientProfileService {

  Optional<PatientDto> find(String token);

  PatientDto create(String token, PatientInputDto input);

  PatientDto update(String token, PatientInputDto input);
}
