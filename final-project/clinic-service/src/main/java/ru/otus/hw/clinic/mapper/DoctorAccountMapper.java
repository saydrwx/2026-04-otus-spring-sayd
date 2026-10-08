package ru.otus.hw.clinic.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.dto.DoctorAccountDto;
import ru.otus.hw.clinic.model.DoctorAccount;

@Component
public class DoctorAccountMapper {

  public DoctorAccountDto toDto(DoctorAccount account) {
    return new DoctorAccountDto(account.getAccountId(), account.getDoctorId());
  }
}
