package ru.otus.hw.repositories;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.TestDataPreparer;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Genre;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Репозиторий на основе Spring Data MongoDB для работы с книгами")
@DataMongoTest
@Import(TestDataPreparer.class)
class BookRepositoryTest {

  @Autowired
  private BookRepository repository;

  @Autowired
  private TestDataPreparer testDataPreparer;

  private List<Author> dbAuthors;

  private List<Genre> dbGenres;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
    dbAuthors = testDataPreparer.getDbAuthors();
    dbGenres = testDataPreparer.getDbGenres();
  }

  @DisplayName("должен загружать книгу по id")
  @Test
  void shouldReturnCorrectBookById() {
    Optional<Book> book = repository.findById("1");

    assertThat(book)
      .isPresent()
      .get()
      .satisfies(actual -> {
        assertThat(actual.getId()).isEqualTo("1");
        assertThat(actual.getTitle()).isEqualTo("BookTitle_1");
      });
  }

  @DisplayName("должен загружать список всех книг")
  @Test
  void shouldReturnCorrectBooksList() {
    var books = repository.findAll();
    assertThat(books).hasSize(3);
  }

  @DisplayName("должен сохранять новую книгу")
  @Test
  void shouldSaveNewBook() {
    var expectedBook = new Book(null, "BookTitle_10500", dbAuthors.get(0),
      List.of(dbGenres.get(0), dbGenres.get(2)));
    var returnedBook = repository.save(expectedBook);
    assertThat(returnedBook).isNotNull()
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedBook);

    assertThat(repository.findById(returnedBook.getId()))
      .isPresent()
      .get()
      .isEqualTo(returnedBook);
  }

  @DisplayName("должен сохранять измененную книгу")
  @Test
  void shouldSaveUpdatedBook() {
    var expectedBook = new Book("1", "BookTitle_10500", dbAuthors.get(2),
      List.of(dbGenres.get(4), dbGenres.get(5)));

    assertThat(repository.findById(expectedBook.getId()))
      .isPresent()
      .get()
      .isNotEqualTo(expectedBook);

    var returnedBook = repository.save(expectedBook);
    assertThat(returnedBook).isNotNull()
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedBook);

    assertThat(repository.findById(returnedBook.getId()))
      .isPresent()
      .get()
      .isEqualTo(returnedBook);
  }

  @DisplayName("должен выкидывать ошибку при изменении несуществующей книги")
  @Test
  void shouldThrowOnUpdatingNonExistentBook() {
    var nonExistentBook = new Book("42", "BookTitle_42", dbAuthors.get(2),
      List.of(dbGenres.get(4), dbGenres.get(5)));

    assertThatThrownBy(() -> repository.save(nonExistentBook))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Book with id %s not found".formatted(nonExistentBook.getId()));
  }

  @DisplayName("должен удалять книгу по id")
  @Test
  void shouldDeleteBook() {
    assertThat(repository.findById("1")).isPresent();
    repository.deleteById("1");
    assertThat(repository.findById("1")).isEmpty();
  }
}