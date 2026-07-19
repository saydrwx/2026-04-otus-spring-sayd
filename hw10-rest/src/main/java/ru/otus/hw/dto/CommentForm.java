package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentForm {

  @NotBlank(message = "Комментарий не должен быть пустым")
  @Size(min = 2, max = 1000, message = "Длина комментария должна быть от 2 до 1000 символов")
  private String text = "";

  public CommentForm(String text) {
    this.text = text;
  }
}
