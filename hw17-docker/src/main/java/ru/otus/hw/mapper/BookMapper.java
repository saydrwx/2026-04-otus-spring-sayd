package ru.otus.hw.mapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.model.Author;
import ru.otus.hw.model.Book;
import ru.otus.hw.model.Genre;

@Component
@RequiredArgsConstructor
public class BookMapper {

  private final AuthorMapper authorMapper;

  private final GenreMapper genreMapper;

  public BookDto toDto(Book book) {
    List<GenreDto> genres = book.getGenres().stream()
      .map(genreMapper::toDto)
      .sorted(Comparator.comparing(GenreDto::name, String.CASE_INSENSITIVE_ORDER))
      .toList();

    return new BookDto(
      book.getId(),
      book.getTitle(),
      authorMapper.toDto(book.getAuthor()),
      genres
    );
  }

  public BookForm toForm(BookDto book) {
    Set<Long> genreIds = book.genres().stream()
      .map(GenreDto::id)
      .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    return new BookForm(book.title(), book.author().id(), genreIds);
  }

  public Book toEntity(BookForm form, Author author, Collection<Genre> genres) {
    return new Book(normalize(form.getTitle()), author, genres);
  }

  public void updateEntity(
    BookForm form,
    Author author,
    Collection<Genre> genres,
    Book book
  ) {
    book.setTitle(normalize(form.getTitle()));
    book.setAuthor(author);
    book.replaceGenres(genres);
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
