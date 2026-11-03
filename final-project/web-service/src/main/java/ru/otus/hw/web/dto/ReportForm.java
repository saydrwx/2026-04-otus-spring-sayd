package ru.otus.hw.web.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportForm {

  @NotNull
  private UUID appointmentId;

  @NotBlank
  @Size(max = 10000)
  private String findings;

  @NotBlank
  @Size(max = 10000)
  private String diagnosis;

  @NotBlank
  @Size(max = 10000)
  private String recommendations;

  public ReportRequest input() {
    return new ReportRequest(findings, diagnosis, recommendations);
  }

  @Override
  public String toString() {
    return "ReportForm[clinicalData=REDACTED]";
  }
}
