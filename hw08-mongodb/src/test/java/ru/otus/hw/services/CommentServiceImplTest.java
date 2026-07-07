package ru.otus.hw.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.TestDataPreparer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("Сервис для работы с комментариями")
@DataMongoTest
@Import({CommentServiceImpl.class, TestDataPreparer.class})
public class CommentServiceImplTest {

  @Autowired
  private CommentServiceImpl commentService;

  @Autowired
  private TestDataPreparer testDataPreparer;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
  }

  @DisplayName("должен загружать комментарий по id")
  @Test
  void shouldReturnCorrectCommentById() {
    assertDoesNotThrow(() -> commentService.findById("1"));
  }

  @DisplayName("должен загружать список всех комментариев для книги по id")
  @Test
  void shouldReturnCorrectCommentsList() {
    assertDoesNotThrow(() -> commentService.findByBookId("1"));
  }

  @DisplayName("должен сохранять новый комментарий")
  @Test
  void shouldSaveNewComment() {
    assertDoesNotThrow(() -> commentService.insert("comment text", "1"));
  }

  @DisplayName("должен сохранять измененный комментарий")
  @Test
  void shouldSaveUpdatedComment() {
    assertDoesNotThrow(() -> commentService.update("2", "comment text", "1"));
  }

  @DisplayName("должен удалять комментарий по id")
  @Test
  void shouldDeleteComment() {
    assertDoesNotThrow(() -> commentService.deleteById("1"));
  }
}
