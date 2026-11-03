package ru.otus.hw.directory.service;

import java.util.List;
import ru.otus.hw.directory.dto.DoctorDto;
import ru.otus.hw.directory.dto.DoctorInputDto;

public interface DoctorService {

  DoctorDto findById(long id);

  List<DoctorDto> findAll(Long hospitalId, List<Long> specialtyIds);

  DoctorDto create(DoctorInputDto inputDto);

  DoctorDto update(long id, DoctorInputDto inputDto);

  void deleteById(long id);
}
