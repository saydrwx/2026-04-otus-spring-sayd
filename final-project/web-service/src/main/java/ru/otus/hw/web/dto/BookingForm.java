package ru.otus.hw.web.dto;

import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class BookingForm {

  @NotNull
  @Positive
  private Long doctorId;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime start;

  public BookingRequest input() {
    return new BookingRequest(doctorId, start);
  }

  @Override
  public String toString() {
    return "BookingForm[details=REDACTED]";
  }
}
