package ru.otus.hw.services.mongo;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.repositories.mongo.AuthorMongoRepository;
import ru.otus.hw.repositories.mongo.BookMongoRepository;
import ru.otus.hw.repositories.mongo.GenreMongoRepository;

@RequiredArgsConstructor
@Service
public class BookMongoServiceImpl implements BookMongoService {

  private final AuthorMongoRepository authorRepository;

  private final GenreMongoRepository genreRepository;

  private final BookMongoRepository bookRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<BookMongo> findById(String id) {
    return bookRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BookMongo> findAll() {
    return bookRepository.findAll();
  }

  @Override
  @Transactional
  public BookMongo insert(String title, String authorId, Set<String> genresIds) {
    return save(null, title, authorId, genresIds);
  }

  @Override
  @Transactional
  public BookMongo update(String id, String title, String authorId, Set<String> genresIds) {
    return save(id, title, authorId, genresIds);
  }

  @Override
  @Transactional
  public void deleteById(String id) {
    bookRepository.deleteById(id);
  }

  private BookMongo save(String id, String title, String authorId, Set<String> genresIds) {
    if (isEmpty(genresIds)) {
      throw new IllegalArgumentException("Genres ids must not be null");
    }

    var author = authorRepository.findById(authorId)
      .orElseThrow(
        () -> new EntityNotFoundException("Author with id %s not found".formatted(authorId)));
    var genres = genreRepository.findAllById(genresIds);
    if (isEmpty(genres) || genresIds.size() != genres.size()) {
      throw new EntityNotFoundException(
        "One or all genres with ids %s not found".formatted(genresIds));
    }

    var book = new BookMongo(id, title, author, genres);
    return bookRepository.save(book);
  }
}
