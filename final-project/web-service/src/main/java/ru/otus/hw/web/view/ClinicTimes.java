package ru.otus.hw.web.view;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("clinicTimes")
public class ClinicTimes {

  // Mirrors the existing clinic-service time contract; never interpret a slot in the browser's zone.
  public static final ZoneOffset OFFSET = ZoneOffset.ofHours(3);
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(
    "dd MMM uuuu, HH:mm XXX", Locale.forLanguageTag("ru-RU"));
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm",
    Locale.forLanguageTag("ru-RU"));

  public String calendarDate(LocalDate value) {
    return value.format(DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.forLanguageTag("ru-RU")));
  }

  public String dateTime(OffsetDateTime value) {
    return value.withOffsetSameInstant(OFFSET).format(DATE_TIME);
  }

  public String instant(Instant value) {
    return dateTime(value.atOffset(OFFSET));
  }

  public String time(OffsetDateTime value) {
    return value.withOffsetSameInstant(OFFSET).format(TIME);
  }

  public String time(LocalTime value) {
    return value.format(TIME);
  }

  public LocalDate date(OffsetDateTime value) {
    return value.withOffsetSameInstant(OFFSET).toLocalDate();
  }

  public String day(DayOfWeek value) {
    return value.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru-RU"));
  }

  public String status(ru.otus.hw.web.dto.AppointmentStatus value) {
    return switch (value) {
      case BOOKED -> "Запланирован";
      case CANCELLED -> "Отменён";
      case COMPLETED -> "Завершён";
      case NO_SHOW -> "Неявка";
    };
  }
}
