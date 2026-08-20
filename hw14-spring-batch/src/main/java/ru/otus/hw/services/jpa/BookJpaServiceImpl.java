package ru.otus.hw.services.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.jpa.Book;
import ru.otus.hw.repositories.jpa.AuthorJpaRepository;
import ru.otus.hw.repositories.jpa.BookJpaRepository;
import ru.otus.hw.repositories.jpa.GenreJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

@RequiredArgsConstructor
@Service
public class BookJpaServiceImpl implements BookJpaService {

  private final AuthorJpaRepository authorRepository;

  private final GenreJpaRepository genreRepository;

  private final BookJpaRepository bookRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<Book> findById(long id) {
    return bookRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Book> findAll() {
    return bookRepository.findAll();
  }

  @Override
  @Transactional
  public Book insert(String title, long authorId, Set<Long> genresIds) {
    return save(0, title, authorId, genresIds);
  }

  @Override
  @Transactional
  public Book update(long id, String title, long authorId, Set<Long> genresIds) {
    return save(id, title, authorId, genresIds);
  }

  @Override
  @Transactional
  public void deleteById(long id) {
    bookRepository.deleteById(id);
  }

  private Book save(long id, String title, long authorId, Set<Long> genresIds) {
    if (isEmpty(genresIds)) {
      throw new IllegalArgumentException("Genres ids must not be null");
    }

    var author = authorRepository.findById(authorId)
      .orElseThrow(
        () -> new EntityNotFoundException("Author with id %d not found".formatted(authorId)));
    var genres = genreRepository.findAllById(genresIds);
    if (isEmpty(genres) || genresIds.size() != genres.size()) {
      throw new EntityNotFoundException(
        "One or all genres with ids %s not found".formatted(genresIds));
    }

    var book = new Book(id, title, author, genres);
    return bookRepository.save(book);
  }
}
