package ru.otus.hw.converters.mongo;

import org.springframework.stereotype.Component;
import ru.otus.hw.models.mongo.AuthorMongo;

@Component
public class AuthorMongoConverter {

  public String authorToString(AuthorMongo author) {
    return "Id: %s, FullName: %s".formatted(author.getId(), author.getFullName());
  }
}
