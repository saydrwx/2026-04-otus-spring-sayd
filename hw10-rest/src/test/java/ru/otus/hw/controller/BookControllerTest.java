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
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.exception.BookNotFoundException;
import ru.otus.hw.service.BookService;

@WebMvcTest(BookController.class)
class BookControllerTest {

  private static final long BOOK_ID = 20L;

  private static final AuthorDto AUTHOR = new AuthorDto(1L, "Leo Tolstoy");

  private static final GenreDto GENRE = new GenreDto(10L, "Novel");

  private static final BookDto BOOK = new BookDto(
      BOOK_ID,
      "War and Peace",
      AUTHOR,
      List.of(GENRE)
  );

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BookService bookService;

  @Test
  void shouldReturnAllBooks() throws Exception {
    when(bookService.findAll()).thenReturn(List.of(BOOK));

    mockMvc.perform(get("/api/books"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(BOOK_ID))
        .andExpect(jsonPath("$[0].author.fullName").value("Leo Tolstoy"))
        .andExpect(jsonPath("$[0].genres[0].name").value("Novel"));

    verify(bookService).findAll();
  }

  @Test
  void shouldReturnBookById() throws Exception {
    when(bookService.findById(BOOK_ID)).thenReturn(BOOK);

    mockMvc.perform(get("/api/books/{id}", BOOK_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(BOOK_ID))
        .andExpect(jsonPath("$.title").value("War and Peace"));

    verify(bookService).findById(BOOK_ID);
  }

  @Test
  void shouldCreateBook() throws Exception {
    when(bookService.create(any(BookForm.class))).thenReturn(BOOK);

    mockMvc.perform(post("/api/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"War and Peace","authorId":1,"genreIds":[10]}
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/books/" + BOOK_ID))
        .andExpect(jsonPath("$.id").value(BOOK_ID));

    var captor = ArgumentCaptor.forClass(BookForm.class);
    verify(bookService).create(captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("War and Peace");
    assertThat(captor.getValue().getAuthorId()).isEqualTo(1L);
    assertThat(captor.getValue().getGenreIds()).containsExactly(10L);
  }

  @Test
  void shouldRejectInvalidBook() throws Exception {
    mockMvc.perform(post("/api/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\" \",\"authorId\":null,\"genreIds\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.title").isNotEmpty())
        .andExpect(jsonPath("$.errors.authorId").isNotEmpty())
        .andExpect(jsonPath("$.errors.genreIds").isNotEmpty());

    verify(bookService, never()).create(any(BookForm.class));
  }

  @Test
  void shouldUpdateBook() throws Exception {
    var updated = new BookDto(BOOK_ID, "War and Peace II", AUTHOR, List.of(GENRE));
    when(bookService.update(eq(BOOK_ID), any(BookForm.class))).thenReturn(updated);

    mockMvc.perform(put("/api/books/{id}", BOOK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"War and Peace II","authorId":1,"genreIds":[10]}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("War and Peace II"));

    var captor = ArgumentCaptor.forClass(BookForm.class);
    verify(bookService).update(eq(BOOK_ID), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("War and Peace II");
  }

  @Test
  void shouldDeleteBook() throws Exception {
    mockMvc.perform(delete("/api/books/{id}", BOOK_ID))
        .andExpect(status().isNoContent());

    verify(bookService).deleteById(BOOK_ID);
  }

  @Test
  void shouldReturnProblemDetailWhenBookDoesNotExist() throws Exception {
    when(bookService.findById(BOOK_ID)).thenThrow(new BookNotFoundException(BOOK_ID));

    mockMvc.perform(get("/api/books/{id}", BOOK_ID).locale(Locale.ENGLISH))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail")
            .value("Book with id " + BOOK_ID + " was not found"))
        .andExpect(jsonPath("$.instance").value("/api/books/" + BOOK_ID));

    verify(bookService).findById(BOOK_ID);
  }
}
