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

@DisplayName("Сервис для работы с книгами")
@DataMongoTest
@Import({BookServiceImpl.class, TestDataPreparer.class})
public class BookServiceImplTest {

  @Autowired
  private BookServiceImpl bookService;

  @Autowired
  private TestDataPreparer testDataPreparer;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
  }

  @DisplayName("должен загружать книгу по id")
  @Test
  void shouldReturnCorrectBookById() {
    assertDoesNotThrow(() -> bookService.findById("1"));
  }

  @DisplayName("должен загружать список всех книг")
  @Test
  void shouldReturnCorrectBooksList() {
    assertDoesNotThrow(() -> bookService.findAll());
  }

  @DisplayName("должен сохранять новую книгу")
  @Test
  void shouldSaveNewBook() {
    assertDoesNotThrow(() -> bookService.insert("BookTitle_10500", "1",
      Set.of("5", "6")));
  }

  @DisplayName("должен сохранять измененную книгу")
  @Test
  void shouldSaveUpdatedBook() {
    assertDoesNotThrow(() -> bookService.update("1", "BookTitle_10500", "1",
      Set.of("5", "6")));
  }

  @DisplayName("должен удалять книгу по id")
  @Test
  void shouldDeleteBook() {
    assertDoesNotThrow(() -> bookService.deleteById("1"));
  }
}
