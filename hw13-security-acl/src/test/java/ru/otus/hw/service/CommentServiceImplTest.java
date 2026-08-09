package ru.otus.hw.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.mapper.CommentMapper;

@DisplayName("Сервис для работы с комментариями")
@DataJpaTest
@Import({CommentServiceImpl.class, CommentMapper.class})
public class CommentServiceImplTest {

  @Autowired
  private CommentServiceImpl commentService;

  @MockitoBean
  private AclServiceWrapperService aclService;

  @DisplayName("должен загружать комментарий по id")
  @Test
  void shouldReturnCorrectCommentById() {
    assertDoesNotThrow(() -> commentService.findById(1L));
  }

  @DisplayName("должен загружать список всех комментариев для книги по id")
  @Test
  void shouldReturnCorrectCommentsList() {
    assertDoesNotThrow(() -> commentService.findAllByBookId(1L));
  }

  @DisplayName("должен сохранять новый комментарий")
  @Test
  void shouldSaveNewComment() {
    assertDoesNotThrow(() -> commentService.create(1L, new CommentForm("comment text")));
  }

  @DisplayName("должен сохранять измененный комментарий")
  @Test
  void shouldSaveUpdatedComment() {
    assertDoesNotThrow(() -> commentService.update(2L, new CommentForm("comment text")));
  }

  @DisplayName("должен удалять комментарий по id")
  @Test
  void shouldDeleteComment() {
    assertDoesNotThrow(() -> commentService.deleteById(1L));
  }
}
