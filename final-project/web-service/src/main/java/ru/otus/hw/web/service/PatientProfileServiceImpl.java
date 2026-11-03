package ru.otus.hw.web.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.web.client.PatientClient;
import ru.otus.hw.web.dto.PatientDto;
import ru.otus.hw.web.dto.PatientInputDto;

@RequiredArgsConstructor
@Service
public class PatientProfileServiceImpl implements PatientProfileService {

  private final PatientClient client;

  public Optional<PatientDto> find(String token) {
    return client.find(token);
  }

  public PatientDto create(String token, PatientInputDto input) {
    return client.create(token, input);
  }

  public PatientDto update(String token, PatientInputDto input) {
    return client.update(token, input);
  }
}
