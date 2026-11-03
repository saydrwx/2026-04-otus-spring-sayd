package ru.otus.hw.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ProfileForm {

  @NotBlank
  @Size(max = 100)
  private String firstName;

  @NotBlank
  @Size(max = 100)
  private String lastName;

  @Size(max = 100)
  private String middleName;

  @NotNull
  @Past
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate birthDate;

  public PatientInputDto input() {
    return new PatientInputDto(firstName, lastName, middleName, birthDate);
  }

  public static ProfileForm from(PatientDto patient) {
    var form = new ProfileForm();
    form.setFirstName(patient.firstName());
    form.setLastName(patient.lastName());
    form.setMiddleName(patient.middleName());
    form.setBirthDate(patient.birthDate());
    return form;
  }

  @Override
  public String toString() {
    return "ProfileForm[personalData=REDACTED]";
  }
}
