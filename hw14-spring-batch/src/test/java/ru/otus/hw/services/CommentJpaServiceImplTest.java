package ru.otus.hw.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.services.jpa.CommentJpaServiceImpl;

@DisplayName("Сервис для работы с комментариями")
@DataJpaTest
@Import(CommentJpaServiceImpl.class)
public class CommentJpaServiceImplTest {

  @Autowired
  private CommentJpaServiceImpl commentService;

  @DisplayName("должен загружать комментарий по id")
  @Test
  void shouldReturnCorrectCommentById() {
    assertDoesNotThrow(() -> commentService.findById(1L));
  }

  @DisplayName("должен загружать список всех комментариев для книги по id")
  @Test
  void shouldReturnCorrectCommentsList() {
    assertDoesNotThrow(() -> commentService.findByBookId(1L));
  }

  @DisplayName("должен сохранять новый комментарий")
  @Test
  void shouldSaveNewComment() {
    assertDoesNotThrow(() -> commentService.insert("comment text", 1L));
  }

  @DisplayName("должен сохранять измененный комментарий")
  @Test
  void shouldSaveUpdatedComment() {
    assertDoesNotThrow(() -> commentService.update(2L, "comment text", 1L));
  }

  @DisplayName("должен удалять комментарий по id")
  @Test
  void shouldDeleteComment() {
    assertDoesNotThrow(() -> commentService.deleteById(1L));
  }
}
