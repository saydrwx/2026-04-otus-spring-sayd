package ru.otus.hw.web.service;

import java.util.List;
import ru.otus.hw.web.dto.DoctorDto;
import ru.otus.hw.web.dto.DoctorSearchInput;
import ru.otus.hw.web.dto.HospitalDto;
import ru.otus.hw.web.dto.SpecialtyDto;

public interface DirectoryService {

  List<HospitalDto> hospitals();

  List<SpecialtyDto> specialties();

  List<DoctorDto> doctors(DoctorSearchInput input);

  DoctorDto doctor(long id);
}
