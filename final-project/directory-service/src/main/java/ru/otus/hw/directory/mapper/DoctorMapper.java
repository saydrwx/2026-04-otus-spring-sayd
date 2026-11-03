package ru.otus.hw.directory.mapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.directory.dto.DoctorDto;
import ru.otus.hw.directory.dto.DoctorInputDto;
import ru.otus.hw.directory.dto.SpecialtyDto;
import ru.otus.hw.directory.model.Doctor;
import ru.otus.hw.directory.model.Hospital;
import ru.otus.hw.directory.model.Specialty;

@RequiredArgsConstructor
@Component
public class DoctorMapper {

  private final HospitalMapper hospitalMapper;

  private final SpecialtyMapper specialtyMapper;

  public DoctorDto toDto(Doctor doctor) {
    List<SpecialtyDto> specialties = doctor.getSpecialties().stream()
      .map(specialtyMapper::toDto)
      .sorted(Comparator.comparing(SpecialtyDto::name, String.CASE_INSENSITIVE_ORDER))
      .toList();

    return new DoctorDto(
      doctor.getId(),
      doctor.getFirstName(),
      doctor.getLastName(),
      doctor.getEducation(),
      doctor.getExperienceYears(),
      doctor.getEmail(),
      hospitalMapper.toDto(doctor.getHospital()),
      specialties
    );
  }

  public Doctor toEntity(
    DoctorInputDto inputDto,
    Hospital hospital,
    Collection<Specialty> specialties
  ) {
    return new Doctor(
      normalize(inputDto.firstName()),
      normalize(inputDto.lastName()),
      normalize(inputDto.education()),
      inputDto.experienceYears(),
      normalize(inputDto.email()),
      hospital,
      specialties
    );
  }

  public void updateEntity(
    Doctor doctor,
    DoctorInputDto inputDto,
    Hospital hospital,
    Collection<Specialty> specialties
  ) {
    doctor.setFirstName(normalize(inputDto.firstName()));
    doctor.setLastName(normalize(inputDto.lastName()));
    doctor.setEducation(normalize(inputDto.education()));
    doctor.setExperienceYears(inputDto.experienceYears());
    doctor.setEmail(normalize(inputDto.email()));
    doctor.setHospital(hospital);
    doctor.replaceSpecialties(specialties);
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
