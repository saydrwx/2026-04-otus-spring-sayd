package ru.otus.hw.repositories;

import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Genre;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Репозиторий на основе JPA для работы с книгами")
@DataJpaTest
@Import({JpaBookRepository.class})
class JpaBookRepositoryTest {

  @Autowired
  private JpaBookRepository repository;

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
    var actualBooks = repository.findAll();
    var expectedBooks = dbBooks;

    assertThat(actualBooks).containsExactlyElementsOf(expectedBooks);
    actualBooks.forEach(System.out::println);
  }

  @DisplayName("должен сохранять новую книгу")
  @Test
  void shouldSaveNewBook() {
    var expectedBook = new Book(0, "BookTitle_10500", dbAuthors.get(0),
      List.of(dbGenres.get(0), dbGenres.get(2)));
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
      List.of(dbGenres.get(4), dbGenres.get(5)));

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

  @DisplayName("должен выкидывать ошибку при изменении несуществующей книги")
  @Test
  void shouldThrowOnUpdatingNonExistentBook() {
    var nonExistentBook = new Book(42L, "BookTitle_42", dbAuthors.get(2),
      List.of(dbGenres.get(4), dbGenres.get(5)));

    assertThatThrownBy(() -> repository.save(nonExistentBook))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Book with id %d not found".formatted(nonExistentBook.getId()));
  }

  @DisplayName("должен удалять книгу по id")
  @Test
  void shouldDeleteBook() {
    assertThat(repository.findById(1L)).isPresent();
    repository.deleteById(1L);
    assertThat(repository.findById(1L)).isEmpty();
  }

  private static List<Author> getDbAuthors() {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Author(id, "Author_" + id))
      .toList();
  }

  private static List<Genre> getDbGenres() {
    return IntStream.range(1, 7).boxed()
      .map(id -> new Genre(id, "Genre_" + id))
      .toList();
  }

  private static List<Book> getDbBooks(List<Author> dbAuthors, List<Genre> dbGenres) {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Book(id,
        "BookTitle_" + id,
        dbAuthors.get(id - 1),
        dbGenres.subList((id - 1) * 2, (id - 1) * 2 + 2)
      ))
      .toList();
  }

  private static List<Book> getDbBooks() {
    var dbAuthors = getDbAuthors();
    var dbGenres = getDbGenres();
    return getDbBooks(dbAuthors, dbGenres);
  }
}