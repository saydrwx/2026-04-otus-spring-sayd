package ru.otus.hw.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.model.Genre;

@Component
public class GenreMapper {

  public GenreDto toDto(Genre genre) {
    return new GenreDto(genre.getId(), genre.getName());
  }

  public GenreForm toForm(GenreDto genre) {
    return new GenreForm(genre.name());
  }

  public Genre toEntity(GenreForm form) {
    return new Genre(normalize(form.getName()));
  }

  public void updateEntity(GenreForm form, Genre genre) {
    genre.setName(normalize(form.getName()));
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
