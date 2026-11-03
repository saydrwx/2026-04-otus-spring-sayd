package ru.otus.hw.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.web.client.RegistrationClient;
import ru.otus.hw.web.dto.RegistrationRequest;

@RequiredArgsConstructor
@Service
public class RegistrationServiceImpl implements RegistrationService {

  private final RegistrationClient client;

  @Override
  public void register(RegistrationRequest input) {
    client.register(input);
  }
}
