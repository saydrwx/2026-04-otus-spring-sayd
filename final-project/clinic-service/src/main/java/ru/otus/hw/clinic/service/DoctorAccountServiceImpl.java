package ru.otus.hw.clinic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.dto.DoctorAccountDto;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.mapper.DoctorAccountMapper;
import ru.otus.hw.clinic.repository.DoctorAccountRepository;
import ru.otus.hw.clinic.security.CurrentAccount;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorAccountServiceImpl implements DoctorAccountService {

  private final DoctorAccountRepository doctorAccountRepository;

  private final DoctorAccountMapper doctorAccountMapper;

  private final CurrentAccount currentAccount;

  @Override
  public DoctorAccountDto findCurrent() {
    return doctorAccountRepository.findById(currentAccount.id())
      .map(doctorAccountMapper::toDto)
      .orElseThrow(
        () -> new ResourceNotFoundException("Doctor account link has not been configured"));
  }
}
