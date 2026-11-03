package ru.otus.hw.web.client;

import ru.otus.hw.web.dto.RegistrationRequest;

public interface RegistrationClient {

  void register(RegistrationRequest input);
}
