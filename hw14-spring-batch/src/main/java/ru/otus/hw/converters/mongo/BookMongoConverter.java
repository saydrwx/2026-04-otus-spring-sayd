package ru.otus.hw.converters.mongo;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.models.mongo.BookMongo;

@RequiredArgsConstructor
@Component
public class BookMongoConverter {

  private final AuthorMongoConverter authorConverter;

  private final GenreMongoConverter genreConverter;

  public String bookToString(BookMongo book) {
    var genresString = book.getGenres().stream()
      .map(genreConverter::genreToString)
      .map("{%s}"::formatted)
      .collect(Collectors.joining(", "));
    return "Id: %s, title: %s, author: {%s}, genres: [%s]".formatted(
      book.getId(),
      book.getTitle(),
      authorConverter.authorToString(book.getAuthor()),
      genresString);
  }
}
