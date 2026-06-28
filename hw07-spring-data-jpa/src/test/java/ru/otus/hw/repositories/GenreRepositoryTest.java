package ru.otus.hw.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.otus.hw.models.Genre;

@DisplayName("Репозиторий на основе JPA для работы с жанрами")
@DataJpaTest
class GenreRepositoryTest {

  @Autowired
  private GenreRepository repository;

  private List<Genre> dbGenres;

  @BeforeEach
  void setUp() {
    dbGenres = getDbGenres();
  }

  @DisplayName("должен загружать жанры по id")
  @Test
  void shouldReturnCorrectGenresById() {
    var genreIds = Set.of(2L, 3L, 42L);
    var expectedGenres = dbGenres.stream()
      .filter(genre -> genreIds.contains(genre.getId()))
      .toList();

    var actualGenres = repository.findAllById(genreIds);
    assertThat(actualGenres).containsExactlyElementsOf(expectedGenres);

    actualGenres.forEach(System.out::println);
  }

  @DisplayName("должен загружать список всех жанров")
  @Test
  void shouldReturnCorrectGenresList() {
    var actualGenres = repository.findAll();
    var expectedGenres = dbGenres;

    assertThat(actualGenres).containsExactlyElementsOf(expectedGenres);
    actualGenres.forEach(System.out::println);
  }

  private static List<Genre> getDbGenres() {
    return IntStream.range(1, 7).boxed()
      .map(id -> new Genre(id, "Genre_" + id))
      .toList();
  }
}
