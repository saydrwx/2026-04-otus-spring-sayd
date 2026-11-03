package ru.otus.hw.directory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecialtyInputDto(@NotBlank @Size(max = 255) String name) {

}
