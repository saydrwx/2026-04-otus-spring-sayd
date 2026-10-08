package ru.otus.hw.web;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import ru.otus.hw.web.view.ClinicTimes;
import static org.assertj.core.api.Assertions.*;
class ClinicTimesTest {
  @Test void displaysInstantsInTheClinicOffsetInsteadOfTheJvmOrBrowserZone() {
    var times=new ClinicTimes();
    assertThat(times.dateTime(OffsetDateTime.parse("2030-01-02T21:30:00Z"))).isEqualTo("03 янв. 2030, 00:30 +03:00");
    assertThat(times.date(OffsetDateTime.parse("2030-01-02T21:30:00Z"))).hasToString("2030-01-03");
  }
}
