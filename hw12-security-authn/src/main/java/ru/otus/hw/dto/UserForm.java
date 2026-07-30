package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserForm {

  @NotBlank(message = "{user.name.not-blank}")
  @Size(min = 2, max = 255, message = "{user.name.size}")
  private String name;

  @NotBlank(message = "{user.password.not-blank}")
  @Size(min = 5, max = 255, message = "{user.password.size}")
  private String password;
}
