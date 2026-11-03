package ru.otus.hw.authorization.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.otus.hw.authorization.dto.RegistrationDto;
import ru.otus.hw.authorization.dto.RegistrationInputDto;
import ru.otus.hw.authorization.exception.EmailAlreadyRegisteredException;
import ru.otus.hw.authorization.repository.AccountRepository;

@RequiredArgsConstructor
@Service
public class RegistrationServiceImpl implements RegistrationService {

  private final AccountRepository accountRepository;

  private final PasswordEncoder passwordEncoder;

  @Override
  public RegistrationDto register(RegistrationInputDto input) {
    if (accountRepository.emailExists(input.email())) {
      throw new EmailAlreadyRegisteredException();
    }
    String encoded = passwordEncoder.encode(input.password());
    UUID id = UUID.randomUUID();
    if (!accountRepository.insertPatient(id, input.email(), encoded)) {
      throw new EmailAlreadyRegisteredException();
    }
    return new RegistrationDto(id, input.email(), "PATIENT");
  }
}
