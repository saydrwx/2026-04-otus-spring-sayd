package ru.otus.hw.web.client;

import java.util.Optional;
import java.util.UUID;
import ru.otus.hw.web.dto.*;

public interface ClinicalClient {

  ReportDto report(String token, UUID appointmentId);

  ReportPageDto reports(String token, UUID patientId, int page, int size);

  ReportDto submitReport(String token, UUID appointmentId, ReportRequest input);

  void noShow(String token, UUID appointmentId);

  Optional<ReviewDto> review(String token, UUID appointmentId);

  ReviewDto createReview(String token, UUID appointmentId, ReviewRequest input);

  ReviewDto updateReview(String token, UUID appointmentId, ReviewRequest input);

  void deleteReview(String token, UUID appointmentId);
}
