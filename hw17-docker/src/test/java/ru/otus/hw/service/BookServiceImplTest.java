package ru.otus.hw.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.exception.BookNotFoundException;
import ru.otus.hw.mapper.AuthorMapper;
import ru.otus.hw.mapper.BookMapper;
import ru.otus.hw.mapper.GenreMapper;

@DisplayName("Сервис для работы с книгами")
@DataJpaTest
@Import({BookServiceImpl.class, BookMapper.class, AuthorMapper.class, GenreMapper.class})
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
    assertDoesNotThrow(() -> bookService.create(new BookForm("BookTitle_10500", 1L,
      Set.of(5L, 6L))));
  }

  @DisplayName("должен сохранять измененную книгу")
  @Test
  void shouldSaveUpdatedBook() {
    assertDoesNotThrow(() -> bookService.update(
      1L,
      new BookForm("BookTitle_10500", 1L, Set.of(5L, 6L)))
    );
  }

  @DisplayName("должен выкидывать ошибку при изменении несуществующей книги")
  @Test
  void shouldThrowOnUpdatingNonExistentBook() {
    var nonExistentBookId = 42L;
    assertThatThrownBy(() -> bookService.update(
      nonExistentBookId,
      new BookForm("BookTitle_10500", 1L, Set.of(5L, 6L)))
    )
    .isInstanceOf(BookNotFoundException.class)
    .hasMessageContaining("Book with id %d was not found".formatted(nonExistentBookId));
  }

  @DisplayName("должен удалять книгу по id")
  @Test
  void shouldDeleteBook() {
    assertDoesNotThrow(() -> bookService.deleteById(1L));
  }
}
