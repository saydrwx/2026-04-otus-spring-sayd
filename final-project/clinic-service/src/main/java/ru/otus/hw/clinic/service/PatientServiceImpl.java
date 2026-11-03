package ru.otus.hw.clinic.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.clinic.dto.PatientDto;
import ru.otus.hw.clinic.dto.PatientInputDto;
import ru.otus.hw.clinic.exception.ProfileAlreadyExistsException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.mapper.PatientMapper;
import ru.otus.hw.clinic.model.Patient;
import ru.otus.hw.clinic.repository.PatientRepository;
import ru.otus.hw.clinic.security.CurrentAccount;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('PATIENT')")
public class PatientServiceImpl implements PatientService {

  private final PatientRepository patientRepository;

  private final PatientMapper patientMapper;

  private final CurrentAccount currentAccount;

  @Override
  public PatientDto findCurrent() {
    return patientMapper.toDto(findPatient());
  }

  @Override
  @Transactional
  public PatientDto createCurrent(PatientInputDto input) {
    UUID accountId = currentAccount.id();
    if (patientRepository.existsByAccountId(accountId)) {
      throw new ProfileAlreadyExistsException("Patient profile already exists");
    }
    var patient = new Patient(accountId);
    patientMapper.updateEntity(patient, input);
    return patientMapper.toDto(patientRepository.saveAndFlush(patient));
  }

  @Override
  @Transactional
  public PatientDto updateCurrent(PatientInputDto input) {
    Patient patient = findPatient();
    patientMapper.updateEntity(patient, input);
    patientRepository.flush();
    return patientMapper.toDto(patient);
  }

  private Patient findPatient() {
    return patientRepository.findByAccountId(currentAccount.id())
      .orElseThrow(() -> new ResourceNotFoundException("Patient profile has not been created"));
  }
}
