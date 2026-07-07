package ru.otus.hw.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.TestDataPreparer;

@DisplayName("Сервис для работы с авторами")
@DataMongoTest
@Import({GenreServiceImpl.class, TestDataPreparer.class})
public class GenreServiceImplTest {

  @Autowired
  private GenreServiceImpl genreService;

  @Autowired
  private TestDataPreparer testDataPreparer;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
  }

  @DisplayName("должен загружать жанры по id")
  @Test
  void shouldReturnCorrectGenresById() {
    assertDoesNotThrow(() -> genreService.findAllByIds(Set.of("1", "2")));
  }

  @DisplayName("должен загружать список всех авторов")
  @Test
  void shouldReturnCorrectAuthorsList() {
    assertDoesNotThrow(() -> genreService.findAll());
  }
}
