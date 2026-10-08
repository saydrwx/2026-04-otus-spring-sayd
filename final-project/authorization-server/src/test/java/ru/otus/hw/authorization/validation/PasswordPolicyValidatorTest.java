package ru.otus.hw.authorization.validation;

import org.junit.jupiter.api.Test;
import ru.otus.hw.authorization.dto.RegistrationInputDto;
import ru.otus.hw.authorization.util.EmailAddresses;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyValidatorTest {

  private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

  @Test
  void checksCharacterAndUtf8BoundariesWithoutTruncation() {
    assertThat(validator.isValid("a".repeat(11), null)).isFalse();
    assertThat(validator.isValid("a".repeat(12), null)).isTrue();
    assertThat(validator.isValid("a".repeat(64), null)).isTrue();
    assertThat(validator.isValid("a".repeat(65), null)).isFalse();
    assertThat(validator.isValid("€".repeat(24), null)).isTrue();
    assertThat(validator.isValid("€".repeat(25), null)).isFalse();
    assertThat(validator.isValid("😺".repeat(18), null)).isTrue();
    assertThat(validator.isValid("😺".repeat(19), null)).isFalse();
    assertThat(validator.isValid("a".repeat(12) + (char) 0xD800, null)).isFalse();
  }

  @Test
  void normalizesOnlyEmailAndRedactsCredentialDto() {
    String password = "  A long passphrase  ";
    var input = new RegistrationInputDto("  Patient@Example.Test  ", password);
    assertThat(input.email()).isEqualTo("patient@example.test");
    assertThat(input.password()).isEqualTo(password);
    assertThat(input.toString()).doesNotContain(password).doesNotContain(input.email());
    assertThat(EmailAddresses.normalize(null)).isNull();
  }
}
