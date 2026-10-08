package ru.otus.hw.clinic.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.dto.ReviewDto;
import ru.otus.hw.clinic.model.Review;

@Component
public class ReviewMapper {

  public ReviewDto toDto(Review review) {
    return new ReviewDto(
      review.getId(),
      review.getAppointment().getId(),
      review.getRating(),
      review.getText(),
      review.getCreatedAt(),
      review.getUpdatedAt()
    );
  }
}
