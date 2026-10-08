package ru.otus.hw.directory.service;

import java.util.List;
import ru.otus.hw.directory.dto.HospitalDto;
import ru.otus.hw.directory.dto.HospitalInputDto;

public interface HospitalService {

  HospitalDto findById(long id);

  List<HospitalDto> findAll();

  HospitalDto create(HospitalInputDto inputDto);

  HospitalDto update(long id, HospitalInputDto inputDto);

  void deleteById(long id);
}
