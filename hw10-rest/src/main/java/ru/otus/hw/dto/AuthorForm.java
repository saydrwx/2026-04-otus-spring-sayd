package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthorForm {

  @NotBlank(message = "Имя автора не должно быть пустым")
  @Size(min = 2, max = 255, message = "Длина имени автора должна быть от 2 до 255 символов")
  private String fullName = "";

  public AuthorForm(String fullName) {
    this.fullName = fullName;
  }
}
