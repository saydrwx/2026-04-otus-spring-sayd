package ru.otus.hw.converters.jpa;

import org.springframework.stereotype.Component;
import ru.otus.hw.models.jpa.Author;

@Component
public class AuthorJpaConverter {

  public String authorToString(Author author) {
    return "Id: %d, FullName: %s".formatted(author.getId(), author.getFullName());
  }
}
