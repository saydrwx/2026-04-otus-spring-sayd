package ru.otus.hw.web.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.web.client.ClinicalClient;
import ru.otus.hw.web.dto.ReportDto;
import ru.otus.hw.web.dto.ReportPageDto;
import ru.otus.hw.web.dto.ReportRequest;
import ru.otus.hw.web.dto.ReviewDto;
import ru.otus.hw.web.dto.ReviewRequest;

@RequiredArgsConstructor
@Service
public class ClinicalServiceImpl implements ClinicalService {

  private final ClinicalClient client;

  public ReportDto report(String token, UUID id) {
    return client.report(token, id);
  }

  public ReportPageDto reports(String token, UUID id, int page, int size) {
    return client.reports(token, id, page, size);
  }

  public ReportDto submitReport(String token, UUID id, ReportRequest input) {
    return client.submitReport(token, id, input);
  }

  public void noShow(String token, UUID id) {
    client.noShow(token, id);
  }

  public Optional<ReviewDto> review(String token, UUID id) {
    return client.review(token, id);
  }

  public ReviewDto createReview(String token, UUID id, ReviewRequest input) {
    return client.createReview(token, id, input);
  }

  public ReviewDto updateReview(String token, UUID id, ReviewRequest input) {
    return client.updateReview(token, id, input);
  }

  public void deleteReview(String token, UUID id) {
    client.deleteReview(token, id);
  }
}
