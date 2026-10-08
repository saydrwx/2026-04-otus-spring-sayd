package ru.otus.hw.clinic.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.clinic.dto.ReviewDto;
import ru.otus.hw.clinic.dto.ReviewInputDto;
import ru.otus.hw.clinic.service.ReviewService;

@RequiredArgsConstructor
@RestController
public class ReviewController {

  private final ReviewService reviewService;

  @GetMapping("/api/appointments/{id}/review")
  public ReviewDto find(@PathVariable UUID id) {
    return reviewService.findByAppointmentId(id);
  }

  @PostMapping("/api/appointments/{id}/review")
  public ResponseEntity<ReviewDto> create(@PathVariable UUID id,
    @Valid @RequestBody ReviewInputDto input) {
    return ResponseEntity.created(URI.create("/api/appointments/" + id + "/review"))
      .body(reviewService.create(id, input));
  }

  @PutMapping("/api/appointments/{id}/review")
  public ReviewDto update(@PathVariable UUID id, @Valid @RequestBody ReviewInputDto input) {
    return reviewService.update(id, input);
  }

  @DeleteMapping("/api/appointments/{id}/review")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    reviewService.delete(id);
  }
}
