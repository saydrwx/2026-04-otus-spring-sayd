package ru.otus.hw.web.client;

import java.util.Optional;
import ru.otus.hw.web.dto.PatientDto;
import ru.otus.hw.web.dto.PatientInputDto;

public interface PatientClient {

  Optional<PatientDto> find(String token);

  PatientDto create(String token, PatientInputDto input);

  PatientDto update(String token, PatientInputDto input);
}
