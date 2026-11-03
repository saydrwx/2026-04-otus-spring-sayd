package ru.otus.hw.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorAdminForm {

  @NotBlank
  @Size(max = 255)
  private String firstName;

  @NotBlank
  @Size(max = 255)
  private String lastName;

  @Size(max = 1000)
  private String education;

  @NotNull
  @PositiveOrZero
  private Integer experienceYears;

  @Email
  @Size(max = 255)
  private String email;

  @NotNull
  @Positive
  private Long hospitalId;

  @NotEmpty
  @Size(max = 20)
  private java.util.Set<@NotNull @Positive Long> specialtyIds = new java.util.LinkedHashSet<>();

  public Input input() {
    return new Input(
      firstName,
      lastName,
      education,
      experienceYears,
      email,
      hospitalId,
      specialtyIds
    );
  }

  public record Input(
    String firstName,
    String lastName,
    String education,
    Integer experienceYears,
    String email,
    Long hospitalId,
    Set<Long> specialtyIds
  ) {

  }
}
