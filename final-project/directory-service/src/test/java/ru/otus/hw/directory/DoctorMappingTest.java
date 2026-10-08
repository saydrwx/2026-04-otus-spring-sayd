package ru.otus.hw.directory;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.otus.hw.directory.mapper.*;
import ru.otus.hw.directory.model.*;

import static org.assertj.core.api.Assertions.*;

class DoctorMappingTest {

  @Test
  void nestedSpecialtiesHaveCaseInsensitiveOrderIndependentOfDatabaseCollation() {
    var hospital = new Hospital("Hospital", "Address");
    hospital.setId(1L);
    var neuro = new Specialty("Neurology");
    neuro.setId(2L);
    var cardio = new Specialty("cardiology");
    cardio.setId(3L);
    var doctor = new Doctor("Alex", "Smith", null, 0, null, hospital, List.of(neuro, cardio));
    doctor.setId(4L);
    var mapper = new DoctorMapper(new HospitalMapper(), new SpecialtyMapper());
    assertThat(mapper.toDto(doctor).specialties()).extracting(
        ru.otus.hw.directory.dto.SpecialtyDto::name)
      .containsExactly("cardiology", "Neurology");
    assertThat(doctor.getSpecialties()).containsExactly(neuro, cardio);
  }
}
