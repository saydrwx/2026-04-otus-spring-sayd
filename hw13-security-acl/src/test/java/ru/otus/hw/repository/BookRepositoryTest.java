package ru.otus.hw.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.otus.hw.model.Author;
import ru.otus.hw.model.Book;
import ru.otus.hw.model.Genre;

@DisplayName("Репозиторий на основе JPA для работы с книгами")
@DataJpaTest
class BookRepositoryTest {

  @Autowired
  private BookRepository repository;

  @Autowired
  private TestEntityManager em;

  private List<Author> dbAuthors;

  private List<Genre> dbGenres;

  private List<Book> dbBooks;

  @BeforeEach
  void setUp() {
    dbAuthors = getDbAuthors();
    dbGenres = getDbGenres();
    dbBooks = getDbBooks(dbAuthors, dbGenres);
  }

  @DisplayName("должен загружать книгу по id")
  @ParameterizedTest
  @MethodSource("getDbBooks")
  void shouldReturnCorrectBookById(Book expectedBook) {
    var actualBook = repository.findById(expectedBook.getId());
    assertThat(actualBook).isPresent()
      .get()
      .isEqualTo(expectedBook);
  }

  @DisplayName("должен загружать список всех книг")
  @Test
  void shouldReturnCorrectBooksList() {
    var actualBooks = repository.findAllByOrderByTitleAsc();
    var expectedBooks = dbBooks;

    assertThat(actualBooks).containsExactlyElementsOf(expectedBooks);
    actualBooks.forEach(System.out::println);
  }

  @DisplayName("должен сохранять новую книгу")
  @Test
  void shouldSaveNewBook() {
    var expectedBook = new Book("BookTitle_10500", dbAuthors.get(0),
      Set.of(dbGenres.get(0), dbGenres.get(2)));
    var returnedBook = repository.save(expectedBook);
    assertThat(returnedBook).isNotNull()
      .matches(book -> book.getId() > 0)
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedBook);

    assertThat(Optional.ofNullable(em.find(Book.class, returnedBook.getId())))
      .isPresent()
      .get()
      .isEqualTo(returnedBook);
  }

  @DisplayName("должен сохранять измененную книгу")
  @Test
  void shouldSaveUpdatedBook() {
    var expectedBook = new Book(1L, "BookTitle_10500", dbAuthors.get(2),
      Set.of(dbGenres.get(4), dbGenres.get(5)));

    assertThat(repository.findById(expectedBook.getId()))
      .isPresent()
      .get()
      .isNotEqualTo(expectedBook);

    var returnedBook = repository.save(expectedBook);
    assertThat(returnedBook).isNotNull()
      .matches(book -> book.getId() > 0)
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedBook);

    assertThat(Optional.ofNullable(em.find(Book.class, returnedBook.getId())))
      .isPresent()
      .get()
      .isEqualTo(returnedBook);
  }

  @DisplayName("должен удалять книгу по id")
  @Test
  void shouldDeleteBook() {
    assertThat(repository.findById(1L)).isPresent();
    repository.deleteById(1L);
    assertThat(repository.findById(1L)).isEmpty();
  }

  private static List<Author> getDbAuthors() {
    return LongStream.range(1, 4).boxed()
      .map(id -> new Author(id, "Author_" + id))
      .toList();
  }

  private static List<Genre> getDbGenres() {
    return LongStream.range(1, 7).boxed()
      .map(id -> new Genre(id, "Genre_" + id))
      .toList();
  }

  private static List<Book> getDbBooks(List<Author> dbAuthors, List<Genre> dbGenres) {
    return LongStream.range(1, 4).boxed()
      .map(id -> new Book(id,
        "BookTitle_" + id,
        dbAuthors.get(id.intValue() - 1),
        new HashSet<>(dbGenres.subList((id.intValue() - 1) * 2, (id.intValue() - 1) * 2 + 2))
      ))
      .toList();
  }

  private static List<Book> getDbBooks() {
    var dbAuthors = getDbAuthors();
    var dbGenres = getDbGenres();
    return getDbBooks(dbAuthors, dbGenres);
  }
}