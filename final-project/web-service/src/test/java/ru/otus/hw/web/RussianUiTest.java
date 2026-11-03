package ru.otus.hw.web;

import jakarta.validation.Validation;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;
import ru.otus.hw.web.dto.AppointmentStatus;
import ru.otus.hw.web.dto.HospitalAdminForm;
import ru.otus.hw.web.validation.DocumentFiles;
import ru.otus.hw.web.view.ClinicTimes;
import ru.otus.hw.web.view.UiLabels;
import static org.assertj.core.api.Assertions.assertThat;

class RussianUiTest {

  @Test
  void statusesAndWeekdaysHaveRussianLabels() {
    var times = new ClinicTimes();
    assertThat(times.status(AppointmentStatus.BOOKED)).isEqualTo("Запланирован");
    assertThat(times.status(AppointmentStatus.CANCELLED)).isEqualTo("Отменён");
    assertThat(times.status(AppointmentStatus.COMPLETED)).isEqualTo("Завершён");
    assertThat(times.status(AppointmentStatus.NO_SHOW)).isEqualTo("Неявка");
    assertThat(times.calendarDate(java.time.LocalDate.of(2030, 1, 3))).isEqualTo("03.01.2030");
    assertThat(times.day(DayOfWeek.THURSDAY)).isEqualTo("четверг");
  }

  @Test
  void catalogLabelsAndExperienceUseRussianWordsAndPluralForms() {
    var labels = new UiLabels();
    assertThat(labels.catalog("hospitals")).isEqualTo("Больницы");
    assertThat(labels.catalog("specialties")).isEqualTo("Специальности");
    assertThat(labels.catalog("doctors")).isEqualTo("Врачи");
    assertThat(labels.experience(0)).isEqualTo("0 лет");
    assertThat(labels.experience(1)).isEqualTo("1 год");
    assertThat(labels.experience(2)).isEqualTo("2 года");
    assertThat(labels.experience(11)).isEqualTo("11 лет");
    assertThat(labels.experience(21)).isEqualTo("21 год");
    assertThat(labels.experience(24)).isEqualTo("24 года");
  }

  @Test
  void documentSizeAndFallbackLabelsAreRussian() {
    var files = new DocumentFiles();
    assertThat(files.sizeLabel(100)).isEqualTo("100 байт");
    assertThat(files.sizeLabel(1024)).isEqualTo("1,0 КиБ");
    assertThat(files.sizeLabel(1024 * 1024)).isEqualTo("1,0 МиБ");
    assertThat(files.typeLabel("application/octet-stream")).isEqualTo("Документ");
  }

  @Test
  void defaultConstraintBundleProvidesRussianMessages() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var violations = factory.getValidator().validate(new HospitalAdminForm());
      assertThat(violations).hasSize(2);
      assertThat(violations).allSatisfy(violation ->
          assertThat(violation.getMessage()).isEqualTo("Поле обязательно для заполнения"));
    }
  }
}
