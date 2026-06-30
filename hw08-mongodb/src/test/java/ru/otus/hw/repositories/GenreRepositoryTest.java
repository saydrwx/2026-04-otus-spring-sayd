package ru.otus.hw.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.TestDataPreparer;
import ru.otus.hw.models.Genre;

@DisplayName("Репозиторий на основе Spring Data MongoDB для работы с жанрами")
@DataMongoTest
@Import(TestDataPreparer.class)
class GenreRepositoryTest {

  @Autowired
  private GenreRepository repository;

  @Autowired
  private TestDataPreparer testDataPreparer;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
  }

  @DisplayName("должен загружать жанры по id")
  @Test
  void shouldReturnCorrectGenresById() {
    var genreIds = Set.of("2", "3", "42");
    var genres = repository.findAllById(genreIds);
    assertThat(genres)
      .hasSize(2)
      .extracting(Genre::getId, Genre::getName)
      .containsExactlyInAnyOrder(
        tuple("2", "Genre_2"),
        tuple("3", "Genre_3")
      );
  }

  @DisplayName("должен загружать список всех жанров")
  @Test
  void shouldReturnCorrectGenresList() {
    var genres = repository.findAll();

    assertThat(genres)
      .hasSize(6)
      .extracting(Genre::getId, Genre::getName)
      .containsExactlyInAnyOrder(
        tuple("1", "Genre_1"),
        tuple("2", "Genre_2"),
        tuple("3", "Genre_3"),
        tuple("4", "Genre_4"),
        tuple("5", "Genre_5"),
        tuple("6", "Genre_6")
      );
  }
}
