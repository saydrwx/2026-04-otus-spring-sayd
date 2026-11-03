package ru.otus.hw.directory;

import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import ru.otus.hw.directory.model.*;

import static org.assertj.core.api.Assertions.*;

class EntityIdentityTest {

  @Test
  void hospitalIdentitySurvivesPersistenceAndBusinessFieldChanges() {
    verifyIdentity(new Hospital(), new Hospital(), Hospital::setId, h -> h.setName("Changed"));
  }

  @Test
  void specialtyIdentitySurvivesPersistenceAndBusinessFieldChanges() {
    verifyIdentity(new Specialty(), new Specialty(), Specialty::setId, s -> s.setName("Changed"));
  }

  @Test
  void doctorIdentityDoesNotDependOnMutableNamesOrAssociations() {
    verifyIdentity(new Doctor(), new Doctor(), Doctor::setId, d -> {
      d.setFirstName("Changed");
      d.setHospital(new Hospital("Other", "Address"));
      d.replaceSpecialties(java.util.List.of(new Specialty("Other")));
    });
  }

  private <T> void verifyIdentity(T first, T second, BiConsumer<T, Long> assignId,
    Consumer<T> mutate) {
    assertThat(first).isNotEqualTo(second);
    var values = new HashSet<T>();
    values.add(first);
    assignId.accept(first, 10L);
    mutate.accept(first);
    assertThat(values).contains(
      first); // Hash must remain stable when the generated ID is assigned.
    assignId.accept(second, 10L);
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assignId.accept(second, 11L);
    assertThat(first).isNotEqualTo(second);
  }
}
