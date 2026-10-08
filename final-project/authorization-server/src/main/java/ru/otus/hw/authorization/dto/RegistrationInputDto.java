package ru.otus.hw.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.otus.hw.authorization.util.EmailAddresses;
import ru.otus.hw.authorization.validation.PasswordPolicy;

public record RegistrationInputDto(
  @NotBlank @Email @Size(max = 255)
  @Pattern(regexp = "[\\x21-\\x7E]+", message = "Use an ASCII email address without spaces") String email,
  @NotBlank @PasswordPolicy @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password
) {

  public RegistrationInputDto {
    email = EmailAddresses.normalize(email);
  }

  @Override
  public String toString() {
    return "RegistrationInputDto[credentials=REDACTED]";
  }
}
