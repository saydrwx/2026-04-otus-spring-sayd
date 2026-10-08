package ru.otus.hw.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialtyAdminForm {

  @NotBlank
  @Size(max = 255)
  private String name;

  public Input input() {
    return new Input(name);
  }

  public record Input(String name) {

  }
}
