package ru.otus.hw.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.otus.hw.model.Author;

@DisplayName("Репозиторий на основе JPA для работы с авторами")
@DataJpaTest
class AuthorRepositoryTest {

  @Autowired
  private AuthorRepository repository;

  private List<Author> dbAuthors;

  @BeforeEach
  void setUp() {
    dbAuthors = getDbAuthors();
  }

  @DisplayName("должен загружать существующего автора по id")
  @Test
  void shouldReturnExistingAuthorById() {
    var expectedAuthor = dbAuthors.get(1);
    var actualAuthor = repository.findById(expectedAuthor.getId());
    assertThat(actualAuthor).isPresent().get().isEqualTo(expectedAuthor);
  }

  @DisplayName("должен вернуть пустой Optional для несуществующего автора")
  @Test
  void shouldReturnEmptyOptionalForNonExistentAuthor() {
    var id = 4L;
    var actualAuthor = repository.findById(id);
    assertThat(actualAuthor).isEmpty();
  }

  @DisplayName("должен загружать список всех авторов")
  @Test
  void shouldReturnCorrectAuthorsList() {
    var actualAuthors = repository.findAll();
    var expectedAuthors = dbAuthors;

    assertThat(actualAuthors).containsExactlyElementsOf(expectedAuthors);
    actualAuthors.forEach(System.out::println);
  }

  private static List<Author> getDbAuthors() {
    return LongStream.range(1, 4).boxed()
      .map(id -> new Author(id, "Author_" + id))
      .toList();
  }
}
