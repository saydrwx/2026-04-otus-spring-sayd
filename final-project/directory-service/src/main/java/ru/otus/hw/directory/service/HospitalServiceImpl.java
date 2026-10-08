package ru.otus.hw.directory.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.directory.dto.HospitalDto;
import ru.otus.hw.directory.dto.HospitalInputDto;
import ru.otus.hw.directory.exception.HospitalNotFoundException;
import ru.otus.hw.directory.mapper.HospitalMapper;
import ru.otus.hw.directory.model.Hospital;
import ru.otus.hw.directory.repository.HospitalRepository;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class HospitalServiceImpl implements HospitalService {

  private final HospitalRepository hospitalRepository;

  private final HospitalMapper hospitalMapper;

  @Override
  public HospitalDto findById(long id) {
    return hospitalMapper.toDto(findHospital(id));
  }

  @Override
  public List<HospitalDto> findAll() {
    return hospitalRepository.findAllByOrderByNameAsc().stream()
      .map(hospitalMapper::toDto)
      .toList();
  }

  @Override
  @Transactional
  public HospitalDto create(HospitalInputDto inputDto) {
    Hospital hospital = hospitalMapper.toEntity(inputDto);
    Hospital savedHospital = hospitalRepository.save(hospital);
    return hospitalMapper.toDto(savedHospital);
  }

  @Override
  @Transactional
  public HospitalDto update(long id, HospitalInputDto inputDto) {
    Hospital hospital = findHospital(id);
    hospitalMapper.updateEntity(hospital, inputDto);
    return hospitalMapper.toDto(hospital);
  }

  @Override
  @Transactional
  public void deleteById(final long id) {
    Hospital hospital = findHospital(id);
    hospitalRepository.delete(hospital);
  }

  private Hospital findHospital(long id) {
    return hospitalRepository.findById(id).orElseThrow(() -> new HospitalNotFoundException(id));
  }
}
