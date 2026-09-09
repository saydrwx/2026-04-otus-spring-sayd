package ru.otus.hw.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.exception.AuthorNotFoundException;
import ru.otus.hw.exception.BookNotFoundException;
import ru.otus.hw.exception.GenreNotFoundException;
import ru.otus.hw.mapper.BookMapper;
import ru.otus.hw.model.Author;
import ru.otus.hw.model.Book;
import ru.otus.hw.model.Genre;
import ru.otus.hw.repository.AuthorRepository;
import ru.otus.hw.repository.BookRepository;
import ru.otus.hw.repository.GenreRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

  private final BookRepository bookRepository;

  private final AuthorRepository authorRepository;

  private final GenreRepository genreRepository;

  private final BookMapper bookMapper;

  @Override
  public BookDto findById(long id) {
    return bookMapper.toDto(findBook(id));
  }

  @Override
  public List<BookDto> findAll() {
    return bookRepository.findAllByOrderByTitleAsc().stream()
        .map(bookMapper::toDto)
        .toList();
  }

  @Override
  @Transactional
  public BookDto create(BookForm form) {
    Author author = findAuthor(form.getAuthorId());
    List<Genre> genres = findGenres(form.getGenreIds());
    Book book = bookMapper.toEntity(form, author, genres);
    return bookMapper.toDto(bookRepository.save(book));
  }

  @Override
  @Transactional
  public BookDto update(long id, BookForm form) {
    Book book = findBook(id);
    Author author = findAuthor(form.getAuthorId());
    List<Genre> genres = findGenres(form.getGenreIds());
    bookMapper.updateEntity(form, author, genres, book);
    return bookMapper.toDto(book);
  }

  @Override
  @Transactional
  public void deleteById(long id) {
    Book book = findBook(id);
    bookRepository.delete(book);
  }

  private Book findBook(long id) {
    return bookRepository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));
  }

  private Author findAuthor(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Author id must not be null");
    }
    return authorRepository.findById(id)
        .orElseThrow(() -> new AuthorNotFoundException(id));
  }

  private List<Genre> findGenres(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException("At least one genre id is required");
    }

    List<Genre> genres = genreRepository.findAllById(ids);
    Set<Long> foundIds = new HashSet<>(genres.stream().map(Genre::getId).toList());

    ids.stream()
        .filter(id -> !foundIds.contains(id))
        .findFirst()
        .ifPresent(id -> {
          throw new GenreNotFoundException(id);
        });

    return genres;
  }
}
