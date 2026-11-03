package ru.otus.hw.authorization.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.authorization.dto.RegistrationDto;
import ru.otus.hw.authorization.dto.RegistrationInputDto;
import ru.otus.hw.authorization.service.RegistrationService;

@RequiredArgsConstructor
@RestController
public class RegistrationController {

  private final RegistrationService registrationService;

  @PostMapping(value = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationDto register(@NotNull @Valid @RequestBody RegistrationInputDto input) {
    return registrationService.register(input);
  }
}
