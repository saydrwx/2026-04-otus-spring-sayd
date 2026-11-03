package ru.otus.hw.directory.service;

import java.util.List;
import ru.otus.hw.directory.dto.SpecialtyDto;
import ru.otus.hw.directory.dto.SpecialtyInputDto;

public interface SpecialtyService {

  SpecialtyDto findById(long id);

  List<SpecialtyDto> findAll();

  SpecialtyDto create(SpecialtyInputDto inputDto);

  SpecialtyDto update(long id, SpecialtyInputDto inputDto);

  void deleteById(long id);
}
