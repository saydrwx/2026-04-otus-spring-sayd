package ru.otus.hw.web.view;

import java.nio.charset.StandardCharsets;
import ru.otus.hw.web.dto.AppointmentStatisticsDto;

public final class StatisticsCsv {

  private StatisticsCsv() {
  }

  public static byte[] render(AppointmentStatisticsDto statistics) {
    var csv = new StringBuilder(
      "from_inclusive,to_exclusive,timezone,doctor_id,hospital_id,status,count\r\n");
    for (var row : statistics.rows()) {
      csv.append(statistics.from()).append(',').append(statistics.to())
        .append(",UTC+03:00,").append(row.doctorId()).append(',').append(row.hospitalId())
        .append(',')
        .append(row.status().name()).append(',').append(row.count()).append("\r\n");
    }
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }
}
