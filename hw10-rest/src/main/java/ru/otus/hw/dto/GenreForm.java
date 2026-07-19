package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GenreForm {

  @NotBlank(message = "Название жанра не должно быть пустым")
  @Size(min = 2, max = 255, message = "Длина названия жанра должна быть от 2 до 255 символов")
  private String name = "";

  public GenreForm(String name) {
    this.name = name;
  }
}
