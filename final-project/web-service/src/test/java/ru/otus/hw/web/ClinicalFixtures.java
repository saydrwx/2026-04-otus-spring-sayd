package ru.otus.hw.web;

import java.time.Instant;
import java.util.UUID;
import ru.otus.hw.web.dto.*;

final class ClinicalFixtures {

  private ClinicalFixtures() {
  }

  static final UUID REPORT_ID = UUID.fromString("8b1ee5a0-455d-4fa7-aed7-c26ab0aa8384");
  static final UUID REVIEW_ID = UUID.fromString("8dd98e0b-54ce-418b-9378-2de64b7b9e9d");
  static final ReportDto REPORT = new ReportDto(REPORT_ID, AppointmentFixtures.ID,
    AppointmentFixtures.PATIENT, 1, 2,
    "Private findings", "Private diagnosis", "Private recommendations",
    AppointmentFixtures.START.plusMinutes(30).toInstant());
  static final ReviewDto REVIEW = new ReviewDto(REVIEW_ID, AppointmentFixtures.ID, 4,
    "Review comment", REPORT.submittedAt(), REPORT.submittedAt());
  static final AppointmentDto COMPLETED = new AppointmentDto(AppointmentFixtures.ID,
    AppointmentFixtures.PATIENT, 1, 2,
    AppointmentFixtures.START, AppointmentFixtures.START.plusMinutes(30),
    AppointmentStatus.COMPLETED, AppointmentFixtures.NOW, null, REPORT.submittedAt());
  static final String REPORT_JSON = """
    {"id":"8b1ee5a0-455d-4fa7-aed7-c26ab0aa8384","appointmentId":"31558885-0e53-4f91-8aa5-d65eedf35c41",
     "patientId":"a1685736-c25a-4f19-9693-a491084d5bc9","doctorId":1,"hospitalId":2,
     "findings":"Private findings","diagnosis":"Private diagnosis","recommendations":"Private recommendations","submittedAt":"2030-01-03T09:30:00Z"}
    """;
  static final String REVIEW_JSON = """
    {"id":"8dd98e0b-54ce-418b-9378-2de64b7b9e9d","appointmentId":"31558885-0e53-4f91-8aa5-d65eedf35c41",
     "rating":4,"text":"Review comment","createdAt":"2030-01-03T09:30:00Z","updatedAt":"2030-01-03T09:30:00Z"}
    """;
}
