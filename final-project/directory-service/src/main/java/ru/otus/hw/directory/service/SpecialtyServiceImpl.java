package ru.otus.hw.directory.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.directory.dto.SpecialtyDto;
import ru.otus.hw.directory.dto.SpecialtyInputDto;
import ru.otus.hw.directory.exception.SpecialtyNotFoundException;
import ru.otus.hw.directory.mapper.SpecialtyMapper;
import ru.otus.hw.directory.model.Specialty;
import ru.otus.hw.directory.repository.SpecialtyRepository;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class SpecialtyServiceImpl implements SpecialtyService {

  private final SpecialtyRepository specialtyRepository;

  private final SpecialtyMapper specialtyMapper;

  @Override
  public SpecialtyDto findById(long id) {
    return specialtyMapper.toDto(findSpecialty(id));
  }

  @Override
  public List<SpecialtyDto> findAll() {
    return specialtyRepository.findAllByOrderByNameAsc().stream()
      .map(specialtyMapper::toDto)
      .toList();
  }

  @Override
  @Transactional
  public SpecialtyDto create(SpecialtyInputDto inputDto) {
    Specialty specialty = specialtyMapper.toEntity(inputDto);
    Specialty savedSpecialty = specialtyRepository.save(specialty);
    return specialtyMapper.toDto(savedSpecialty);
  }

  @Override
  @Transactional
  public SpecialtyDto update(long id, SpecialtyInputDto inputDto) {
    Specialty specialty = findSpecialty(id);
    specialtyMapper.updateEntity(specialty, inputDto);
    return specialtyMapper.toDto(specialty);
  }

  @Override
  @Transactional
  public void deleteById(final long id) {
    Specialty specialty = findSpecialty(id);
    specialtyRepository.delete(specialty);
  }

  private Specialty findSpecialty(long id) {
    return specialtyRepository.findById(id).orElseThrow(() -> new SpecialtyNotFoundException(id));
  }
}
