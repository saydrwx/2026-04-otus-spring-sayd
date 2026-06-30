package ru.otus.hw.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.TestDataPreparer;
import ru.otus.hw.models.Author;

@DisplayName("Репозиторий на основе Spring Data MongoDB для работы с авторами")
@DataMongoTest
@Import(TestDataPreparer.class)
class AuthorRepositoryTest {

  @Autowired
  private AuthorRepository repository;

  @Autowired
  private TestDataPreparer testDataPreparer;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
  }

  @DisplayName("должен загружать существующего автора по id")
  @Test
  void shouldReturnExistingAuthorById() {
    Optional<Author> author = repository.findById("1");

    assertThat(author)
      .isPresent()
      .get()
      .satisfies(actual -> {
        assertThat(actual.getId()).isEqualTo("1");
        assertThat(actual.getFullName()).isEqualTo("Author_1");
      });
  }

  @DisplayName("должен вернуть пустой Optional для несуществующего автора")
  @Test
  void shouldReturnEmptyOptionalForNonExistentAuthor() {
    var id = "4";
    var actualAuthor = repository.findById(id);
    assertThat(actualAuthor).isEmpty();
  }

  @DisplayName("должен загружать список всех авторов")
  @Test
  void shouldReturnCorrectAuthorsList() {
    List<Author> authors = repository.findAll();

    assertThat(authors)
      .hasSize(3)
      .extracting(Author::getId, Author::getFullName)
      .containsExactlyInAnyOrder(
        tuple("1", "Author_1"),
        tuple("2", "Author_2"),
        tuple("3", "Author_3")
      );
  }
}
