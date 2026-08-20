package ru.otus.hw.converters.jpa;

import org.springframework.stereotype.Component;
import ru.otus.hw.models.jpa.Genre;

@Component
public class GenreJpaConverter {

  public String genreToString(Genre genre) {
    return "Id: %d, Name: %s".formatted(genre.getId(), genre.getName());
  }
}
