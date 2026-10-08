package ru.otus.hw.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HospitalAdminForm {

  @NotBlank
  @Size(max = 255)
  private String name;

  @NotBlank
  @Size(max = 300)
  private String address;

  public Input input() {
    return new Input(name, address);
  }

  public record Input(String name, String address) {

  }
}
