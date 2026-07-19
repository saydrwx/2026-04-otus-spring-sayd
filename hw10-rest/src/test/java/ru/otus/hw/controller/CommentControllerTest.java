package ru.otus.hw.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.service.CommentService;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

  private static final long BOOK_ID = 20L;

  private static final long COMMENT_ID = 30L;

  private static final CommentDto COMMENT = new CommentDto(COMMENT_ID, "Excellent", BOOK_ID);

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CommentService commentService;

  @Test
  void shouldReturnCommentsForBook() throws Exception {
    when(commentService.findAllByBookId(BOOK_ID)).thenReturn(List.of(COMMENT));

    mockMvc.perform(get("/api/books/{bookId}/comments", BOOK_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(COMMENT_ID))
        .andExpect(jsonPath("$[0].bookId").value(BOOK_ID));

    verify(commentService).findAllByBookId(BOOK_ID);
  }

  @Test
  void shouldReturnCommentById() throws Exception {
    when(commentService.findById(COMMENT_ID)).thenReturn(COMMENT);

    mockMvc.perform(get("/api/comments/{commentId}", COMMENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(COMMENT_ID))
        .andExpect(jsonPath("$.text").value("Excellent"));

    verify(commentService).findById(COMMENT_ID);
  }

  @Test
  void shouldCreateComment() throws Exception {
    when(commentService.create(eq(BOOK_ID), any(CommentForm.class))).thenReturn(COMMENT);

    mockMvc.perform(post("/api/books/{bookId}/comments", BOOK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\":\"Excellent\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/comments/" + COMMENT_ID))
        .andExpect(jsonPath("$.id").value(COMMENT_ID));

    var captor = ArgumentCaptor.forClass(CommentForm.class);
    verify(commentService).create(eq(BOOK_ID), captor.capture());
    assertThat(captor.getValue().getText()).isEqualTo("Excellent");
  }

  @Test
  void shouldRejectInvalidComment() throws Exception {
    mockMvc.perform(post("/api/books/{bookId}/comments", BOOK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.text").isNotEmpty());

    verify(commentService, never()).create(eq(BOOK_ID), any(CommentForm.class));
  }

  @Test
  void shouldUpdateComment() throws Exception {
    var updated = new CommentDto(COMMENT_ID, "Updated text", BOOK_ID);
    when(commentService.update(eq(COMMENT_ID), any(CommentForm.class))).thenReturn(updated);

    mockMvc.perform(put("/api/comments/{commentId}", COMMENT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\":\"Updated text\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("Updated text"));

    var captor = ArgumentCaptor.forClass(CommentForm.class);
    verify(commentService).update(eq(COMMENT_ID), captor.capture());
    assertThat(captor.getValue().getText()).isEqualTo("Updated text");
  }

  @Test
  void shouldDeleteComment() throws Exception {
    mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID))
        .andExpect(status().isNoContent());

    verify(commentService).deleteById(COMMENT_ID);
  }
}
