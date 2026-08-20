package ru.otus.hw.converters.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.models.jpa.Book;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class BookJpaConverter {

  private final AuthorJpaConverter authorConverter;

  private final GenreJpaConverter genreConverter;

  public String bookToString(Book book) {
    var genresString = book.getGenres().stream()
      .map(genreConverter::genreToString)
      .map("{%s}"::formatted)
      .collect(Collectors.joining(", "));
    return "Id: %d, title: %s, author: {%s}, genres: [%s]".formatted(
      book.getId(),
      book.getTitle(),
      authorConverter.authorToString(book.getAuthor()),
      genresString);
  }
}
