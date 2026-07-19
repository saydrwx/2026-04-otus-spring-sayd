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
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.service.GenreService;

@WebMvcTest(GenreController.class)
class GenreControllerTest {

  private static final long GENRE_ID = 10L;

  private static final GenreDto GENRE = new GenreDto(GENRE_ID, "Novel");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private GenreService genreService;

  @Test
  void shouldReturnAllGenres() throws Exception {
    when(genreService.findAll()).thenReturn(List.of(GENRE));

    mockMvc.perform(get("/api/genres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(GENRE_ID))
        .andExpect(jsonPath("$[0].name").value("Novel"));

    verify(genreService).findAll();
  }

  @Test
  void shouldReturnGenreById() throws Exception {
    when(genreService.findById(GENRE_ID)).thenReturn(GENRE);

    mockMvc.perform(get("/api/genres/{id}", GENRE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(GENRE_ID))
        .andExpect(jsonPath("$.name").value("Novel"));

    verify(genreService).findById(GENRE_ID);
  }

  @Test
  void shouldCreateGenre() throws Exception {
    when(genreService.create(any(GenreForm.class))).thenReturn(GENRE);

    mockMvc.perform(post("/api/genres")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Novel\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/genres/" + GENRE_ID))
        .andExpect(jsonPath("$.name").value("Novel"));

    var captor = ArgumentCaptor.forClass(GenreForm.class);
    verify(genreService).create(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("Novel");
  }

  @Test
  void shouldRejectInvalidGenre() throws Exception {
    mockMvc.perform(post("/api/genres")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").isNotEmpty());

    verify(genreService, never()).create(any(GenreForm.class));
  }

  @Test
  void shouldUpdateGenre() throws Exception {
    var updated = new GenreDto(GENRE_ID, "Classic novel");
    when(genreService.update(eq(GENRE_ID), any(GenreForm.class))).thenReturn(updated);

    mockMvc.perform(put("/api/genres/{id}", GENRE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Classic novel\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Classic novel"));

    var captor = ArgumentCaptor.forClass(GenreForm.class);
    verify(genreService).update(eq(GENRE_ID), captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("Classic novel");
  }

  @Test
  void shouldDeleteGenre() throws Exception {
    mockMvc.perform(delete("/api/genres/{id}", GENRE_ID))
        .andExpect(status().isNoContent());

    verify(genreService).deleteById(GENRE_ID);
  }
}
