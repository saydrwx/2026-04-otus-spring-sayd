package ru.otus.hw.directory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HospitalInputDto(
  @NotBlank @Size(max = 255) String name,
  @NotBlank @Size(max = 300) String address
) {

}
