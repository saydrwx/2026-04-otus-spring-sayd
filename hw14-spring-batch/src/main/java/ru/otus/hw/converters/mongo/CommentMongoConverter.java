package ru.otus.hw.converters.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.models.mongo.CommentMongo;

@RequiredArgsConstructor
@Component
public class CommentMongoConverter {

  public String commentToString(CommentMongo comment) {
    return "Id: %s, Text: %s, Book Id: %s".formatted(
      comment.getId(),
      comment.getText(),
      comment.getBook().getId()
    );
  }
}
