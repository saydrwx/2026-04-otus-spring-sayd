package ru.otus.hw.web.client;

import java.util.List;
import ru.otus.hw.web.dto.*;

public interface DirectoryClient {

  List<HospitalDto> hospitals();

  List<SpecialtyDto> specialties();

  List<DoctorDto> doctors(DoctorSearchInput input);

  DoctorDto doctor(long id);
}
