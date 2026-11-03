package ru.otus.hw.directory.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.directory.dto.SpecialtyDto;
import ru.otus.hw.directory.dto.SpecialtyInputDto;
import ru.otus.hw.directory.model.Specialty;

@Component
public class SpecialtyMapper {

  public SpecialtyDto toDto(Specialty specialty) {
    return new SpecialtyDto(specialty.getId(), specialty.getName());
  }

  public Specialty toEntity(SpecialtyInputDto inputDto) {
    return new Specialty(normalize(inputDto.name()));
  }

  public void updateEntity(Specialty specialty, SpecialtyInputDto inputDto) {
    specialty.setName(normalize(inputDto.name()));
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
