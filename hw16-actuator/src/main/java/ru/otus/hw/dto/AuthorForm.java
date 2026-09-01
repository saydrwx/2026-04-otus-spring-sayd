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

  @NotBlank(message = "{author.full-name.not-blank}")
  @Size(min = 2, max = 255, message = "{author.full-name.size}")
  private String fullName = "";

  public AuthorForm(String fullName) {
    this.fullName = fullName;
  }
}
