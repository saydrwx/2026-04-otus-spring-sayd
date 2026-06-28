package ru.otus.hw.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DisplayName("Сервис для работы с книгами")
@DataJpaTest
@Import(BookServiceImpl.class)
public class BookServiceImplTest {

  @Autowired
  private BookServiceImpl bookService;

  @DisplayName("должен загружать книгу по id")
  @Test
  void shouldReturnCorrectBookById() {
    assertDoesNotThrow(() -> bookService.findById(1L));
  }

  @DisplayName("должен загружать список всех книг")
  @Test
  void shouldReturnCorrectBooksList() {
    assertDoesNotThrow(() -> bookService.findAll());
  }

  @DisplayName("должен сохранять новую книгу")
  @Test
  void shouldSaveNewBook() {
    assertDoesNotThrow(() -> bookService.insert("BookTitle_10500", 1L,
      Set.of(5L, 6L)));
  }

  @DisplayName("должен сохранять измененную книгу")
  @Test
  void shouldSaveUpdatedBook() {
    assertDoesNotThrow(() -> bookService.update(1L, "BookTitle_10500", 1L,
      Set.of(5L, 6L)));
  }

  @DisplayName("должен удалять книгу по id")
  @Test
  void shouldDeleteBook() {
    assertDoesNotThrow(() -> bookService.deleteById(1L));
  }
}
