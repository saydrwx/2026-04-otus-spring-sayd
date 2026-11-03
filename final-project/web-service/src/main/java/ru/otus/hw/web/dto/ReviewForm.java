package ru.otus.hw.web.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {

  @NotNull
  private UUID appointmentId;

  @NotNull
  @Min(1)
  @Max(5)
  private Integer rating;

  @Size(max = 2000)
  private String text;

  public ReviewRequest input() {
    return new ReviewRequest(rating, text);
  }

  @Override
  public String toString() {
    return "ReviewForm[clinicalData=REDACTED]";
  }
}
