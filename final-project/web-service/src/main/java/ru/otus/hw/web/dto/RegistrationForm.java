package ru.otus.hw.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ru.otus.hw.web.validation.PasswordPolicy;

@Getter
@Setter
public class RegistrationForm {

  @NotBlank
  @Email
  @Size(max = 255)
  @Pattern(regexp = "[\\x21-\\x7E]+")
  private String email;

  @NotBlank
  @PasswordPolicy
  private String password;

  @NotBlank
  private String confirmPassword;

  public void setEmail(String email) {
    this.email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
  }

  @Override
  public String toString() {
    return "RegistrationForm[credentials=REDACTED]";
  }
}
