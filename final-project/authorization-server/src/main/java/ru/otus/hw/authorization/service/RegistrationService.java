package ru.otus.hw.authorization.service;

import ru.otus.hw.authorization.dto.RegistrationDto;
import ru.otus.hw.authorization.dto.RegistrationInputDto;

public interface RegistrationService {

  RegistrationDto register(RegistrationInputDto input);
}
