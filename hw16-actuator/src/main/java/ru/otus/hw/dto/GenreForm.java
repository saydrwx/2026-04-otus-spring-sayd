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

  @NotBlank(message = "{genre.name.not-blank}")
  @Size(min = 2, max = 255, message = "{genre.name.size}")
  private String name = "";

  public GenreForm(String name) {
    this.name = name;
  }
}
