package ru.otus.hw.web.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.web.client.DirectoryClient;
import ru.otus.hw.web.dto.DoctorDto;
import ru.otus.hw.web.dto.DoctorSearchInput;
import ru.otus.hw.web.dto.HospitalDto;
import ru.otus.hw.web.dto.SpecialtyDto;

@RequiredArgsConstructor
@Service
public class DirectoryServiceImpl implements DirectoryService {

  private final DirectoryClient client;

  @Override
  public List<HospitalDto> hospitals() {
    return client.hospitals();
  }

  @Override
  public List<SpecialtyDto> specialties() {
    return client.specialties();
  }

  @Override
  public List<DoctorDto> doctors(DoctorSearchInput input) {
    return client.doctors(input);
  }

  @Override
  public DoctorDto doctor(long id) {
    return client.doctor(id);
  }
}
