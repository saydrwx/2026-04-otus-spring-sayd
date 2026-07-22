package ru.otus.hw.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.service.AuthorService;

@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

  private static final long AUTHOR_ID = 1L;

  private static final AuthorDto AUTHOR = new AuthorDto(AUTHOR_ID, "Leo Tolstoy");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthorService authorService;

  @Test
  void shouldReturnAllAuthors() throws Exception {
    when(authorService.findAll()).thenReturn(java.util.List.of(AUTHOR));

    mockMvc.perform(get("/api/authors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(AUTHOR_ID))
        .andExpect(jsonPath("$[0].fullName").value("Leo Tolstoy"));

    verify(authorService).findAll();
  }

  @Test
  void shouldReturnAuthorById() throws Exception {
    when(authorService.findById(AUTHOR_ID)).thenReturn(AUTHOR);

    mockMvc.perform(get("/api/authors/{id}", AUTHOR_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(AUTHOR_ID))
        .andExpect(jsonPath("$.fullName").value("Leo Tolstoy"));

    verify(authorService).findById(AUTHOR_ID);
  }

  @Test
  void shouldCreateAuthor() throws Exception {
    when(authorService.create(any(AuthorForm.class))).thenReturn(AUTHOR);

    mockMvc.perform(post("/api/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\"Leo Tolstoy\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/authors/" + AUTHOR_ID))
        .andExpect(jsonPath("$.id").value(AUTHOR_ID));

    var captor = ArgumentCaptor.forClass(AuthorForm.class);
    verify(authorService).create(captor.capture());
    assertThat(captor.getValue().getFullName()).isEqualTo("Leo Tolstoy");
  }

  @Test
  void shouldRejectInvalidAuthor() throws Exception {
    mockMvc.perform(post("/api/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.fullName").isNotEmpty());

    verify(authorService, never()).create(any(AuthorForm.class));
  }

  @Test
  void shouldUpdateAuthor() throws Exception {
    var updated = new AuthorDto(AUTHOR_ID, "L. Tolstoy");
    when(authorService.update(org.mockito.ArgumentMatchers.eq(AUTHOR_ID),
        any(AuthorForm.class))).thenReturn(updated);

    mockMvc.perform(put("/api/authors/{id}", AUTHOR_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\"L. Tolstoy\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("L. Tolstoy"));

    var captor = ArgumentCaptor.forClass(AuthorForm.class);
    verify(authorService).update(org.mockito.ArgumentMatchers.eq(AUTHOR_ID), captor.capture());
    assertThat(captor.getValue().getFullName()).isEqualTo("L. Tolstoy");
  }

  @Test
  void shouldDeleteAuthor() throws Exception {
    mockMvc.perform(delete("/api/authors/{id}", AUTHOR_ID))
        .andExpect(status().isNoContent());

    verify(authorService).deleteById(AUTHOR_ID);
  }
}
