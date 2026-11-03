package ru.otus.hw.directory.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.directory.dto.HospitalDto;
import ru.otus.hw.directory.dto.HospitalInputDto;
import ru.otus.hw.directory.model.Hospital;

@Component
public class HospitalMapper {

  public HospitalDto toDto(Hospital hospital) {
    return new HospitalDto(hospital.getId(), hospital.getName(), hospital.getAddress());
  }

  public Hospital toEntity(HospitalInputDto inputDto) {
    return new Hospital(normalize(inputDto.name()), normalize(inputDto.address()));
  }

  public void updateEntity(Hospital hospital, HospitalInputDto inputDto) {
    hospital.setName(normalize(inputDto.name()));
    hospital.setAddress(normalize(inputDto.address()));
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
