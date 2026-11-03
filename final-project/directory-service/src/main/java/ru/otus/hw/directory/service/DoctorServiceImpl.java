package ru.otus.hw.directory.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.directory.dto.DoctorDto;
import ru.otus.hw.directory.dto.DoctorInputDto;
import ru.otus.hw.directory.exception.DoctorNotFoundException;
import ru.otus.hw.directory.exception.HospitalNotFoundException;
import ru.otus.hw.directory.exception.SpecialtyNotFoundException;
import ru.otus.hw.directory.mapper.DoctorMapper;
import ru.otus.hw.directory.model.Doctor;
import ru.otus.hw.directory.model.Hospital;
import ru.otus.hw.directory.model.Specialty;
import ru.otus.hw.directory.repository.DoctorRepository;
import ru.otus.hw.directory.repository.DoctorSpecifications;
import ru.otus.hw.directory.repository.HospitalRepository;
import ru.otus.hw.directory.repository.SpecialtyRepository;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {

  private static final Sort DOCTOR_ORDER = Sort.by(
    Sort.Order.asc("lastName"),
    Sort.Order.asc("firstName"),
    Sort.Order.asc("id")
  );

  private final DoctorRepository doctorRepository;

  private final HospitalRepository hospitalRepository;

  private final SpecialtyRepository specialtyRepository;

  private final DoctorMapper doctorMapper;

  @Override
  public DoctorDto findById(long id) {
    return doctorMapper.toDto(findDoctor(id));
  }

  @Override
  public List<DoctorDto> findAll(Long hospitalId, List<Long> specialtyIds) {
    return doctorRepository.findAll(
        DoctorSpecifications.matching(hospitalId, specialtyIds),
        DOCTOR_ORDER
      ).stream()
      .map(doctorMapper::toDto)
      .toList();
  }

  @Override
  @Transactional
  public DoctorDto create(DoctorInputDto inputDto) {
    Hospital hospital = findHospital(inputDto.hospitalId());
    List<Specialty> specialties = findSpecialties(inputDto.specialtyIds());
    Doctor doctor = doctorMapper.toEntity(inputDto, hospital, specialties);
    Doctor savedDoctor = doctorRepository.save(doctor);
    return doctorMapper.toDto(savedDoctor);
  }

  @Override
  @Transactional
  public DoctorDto update(long id, DoctorInputDto inputDto) {
    Doctor doctor = findDoctor(id);
    Hospital hospital = findHospital(inputDto.hospitalId());
    List<Specialty> specialties = findSpecialties(inputDto.specialtyIds());
    doctorMapper.updateEntity(doctor, inputDto, hospital, specialties);
    return doctorMapper.toDto(doctor);
  }

  @Override
  @Transactional
  public void deleteById(long id) {
    Doctor doctor = findDoctor(id);
    doctorRepository.delete(doctor);
  }

  private Doctor findDoctor(long id) {
    return doctorRepository.findById(id).orElseThrow(() -> new DoctorNotFoundException(id));
  }

  private Hospital findHospital(Long id) {
    return hospitalRepository.findById(id).orElseThrow(() -> new HospitalNotFoundException(id));
  }

  private List<Specialty> findSpecialties(Collection<Long> ids) {
    List<Specialty> specialties = specialtyRepository.findAllById(ids);
    Set<Long> foundIds = specialties.stream()
      .map(Specialty::getId)
      .collect(Collectors.toSet());

    ids.stream()
      .filter(id -> !foundIds.contains(id))
      .findFirst()
      .ifPresent(id -> {
        throw new SpecialtyNotFoundException(id);
      });

    return specialties;
  }
}
