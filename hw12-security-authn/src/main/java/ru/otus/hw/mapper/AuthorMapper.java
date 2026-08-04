package ru.otus.hw.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.model.Author;

@Component
public final class AuthorMapper {

  public AuthorDto toDto(Author author) {
    return new AuthorDto(author.getId(), author.getFullName());
  }

  public AuthorForm toForm(AuthorDto author) {
    return new AuthorForm(author.fullName());
  }

  public Author toEntity(AuthorForm form) {
    return new Author(normalize(form.getFullName()));
  }

  public void updateEntity(AuthorForm form, Author author) {
    author.setFullName(normalize(form.getFullName()));
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
