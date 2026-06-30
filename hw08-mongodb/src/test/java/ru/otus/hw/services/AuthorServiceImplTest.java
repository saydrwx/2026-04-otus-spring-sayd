package ru.otus.hw.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.TestDataPreparer;

@DisplayName("Сервис для работы с авторами")
@DataMongoTest
@Import({AuthorServiceImpl.class, TestDataPreparer.class})
public class AuthorServiceImplTest {

  @Autowired
  private AuthorServiceImpl authorService;

  @Autowired
  private TestDataPreparer testDataPreparer;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
  }

  @DisplayName("должен загружать автора по id")
  @Test
  void shouldReturnCorrectAuthorById() {
    assertDoesNotThrow(() -> authorService.findById("1"));
  }

  @DisplayName("должен загружать список всех авторов")
  @Test
  void shouldReturnCorrectAuthorsList() {
    assertDoesNotThrow(() -> authorService.findAll());
  }
}
