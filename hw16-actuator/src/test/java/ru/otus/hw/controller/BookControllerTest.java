package ru.otus.hw.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.exception.BookNotFoundException;
import ru.otus.hw.mapper.BookMapper;
import ru.otus.hw.service.AuthorService;
import ru.otus.hw.service.BookService;
import ru.otus.hw.service.CommentService;
import ru.otus.hw.service.GenreService;

@DisplayName("Контроллер для работы с книгами")
@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

  private static final long BOOK_ID = 10L;

  private static final AuthorDto AUTHOR = new AuthorDto(1L, "Лев Толстой");

  private static final List<AuthorDto> AUTHORS = List.of(
      AUTHOR,
      new AuthorDto(2L, "Александр Пушкин")
  );

  private static final GenreDto NOVEL = new GenreDto(100L, "Роман");

  private static final GenreDto CLASSIC = new GenreDto(101L, "Классика");

  private static final List<GenreDto> GENRES = List.of(NOVEL, CLASSIC);

  private static final BookDto BOOK = new BookDto(
      BOOK_ID,
      "Война и мир",
      AUTHOR,
      GENRES
  );

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BookService bookService;

  @MockitoBean
  private AuthorService authorService;

  @MockitoBean
  private GenreService genreService;

  @MockitoBean
  private CommentService commentService;

  @MockitoBean
  private BookMapper bookMapper;

  @DisplayName("GET /books должен вернуть список книг")
  @Test
  void shouldRenderBookListPage() throws Exception {
    var books = List.of(BOOK);
    when(bookService.findAll()).thenReturn(books);

    mockMvc.perform(get("/books"))
        .andExpect(status().isOk())
        .andExpect(view().name("book/list"))
        .andExpect(model().attribute("books", sameInstance(books)));

    verify(bookService).findAll();
  }

  @DisplayName("GET /books/{id} должен вернуть информацию по книге и комментарии к ней")
  @Test
  void shouldRenderBookDetailsPage() throws Exception {
    var comments = List.of(
        new CommentDto(201L, "Супер", BOOK_ID),
        new CommentDto(202L, "Хорошая", BOOK_ID)
    );
    when(bookService.findById(BOOK_ID)).thenReturn(BOOK);
    when(commentService.findAllByBookId(BOOK_ID)).thenReturn(comments);

    mockMvc.perform(get("/books/{id}", BOOK_ID))
        .andExpect(status().isOk())
        .andExpect(view().name("book/details"))
        .andExpect(model().attribute("book", sameInstance(BOOK)))
        .andExpect(model().attribute("comments", sameInstance(comments)));

    verify(bookService).findById(BOOK_ID);
    verify(commentService).findAllByBookId(BOOK_ID);
  }

  @DisplayName("GET /books/new должен вернуть пустую форму со списком авторов и жанров")
  @Test
  void shouldRenderCreatePage() throws Exception {
    stubFormOptions();

    mockMvc.perform(get("/books/new"))
        .andExpect(status().isOk())
        .andExpect(view().name("book/create"))
        .andExpect(model().attribute("bookForm", instanceOf(BookForm.class)))
        .andExpect(model().attribute("authors", sameInstance(AUTHORS)))
        .andExpect(model().attribute("genres", sameInstance(GENRES)));

    verify(authorService).findAll();
    verify(genreService).findAll();
  }

  @DisplayName("POST /books должен создать книгу и перенаправить на ее страницу")
  @Test
  void shouldCreateBookAndRedirectToDetails() throws Exception {
    when(bookService.create(any(BookForm.class))).thenReturn(BOOK);

    mockMvc.perform(post("/books")
            .param("title", "Война и мир")
            .param("authorId", "1")
            .param("genreIds", "100", "101"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/books/" + BOOK_ID))
        .andExpect(flash().attribute("successMessageCode", "book.created"));

    var formCaptor = ArgumentCaptor.forClass(BookForm.class);
    verify(bookService).create(formCaptor.capture());

    BookForm submittedForm = formCaptor.getValue();
    assertThat(submittedForm.getTitle()).isEqualTo("Война и мир");
    assertThat(submittedForm.getAuthorId()).isEqualTo(1L);
    assertThat(submittedForm.getGenreIds()).containsExactlyInAnyOrder(100L, 101L);

    verifyNoInteractions(authorService, genreService);
  }

  @DisplayName("POST /books должен заново показать страницу создания книги при непрохождении валидации")
  @Test
  void shouldRedisplayCreatePageForInvalidForm() throws Exception {
    stubFormOptions();

    mockMvc.perform(post("/books")
            .param("title", " "))
        .andExpect(status().isOk())
        .andExpect(view().name("book/create"))
        .andExpect(model().attributeHasFieldErrors(
            "bookForm",
            "title",
            "authorId",
            "genreIds"
        ))
        .andExpect(model().attribute("authors", sameInstance(AUTHORS)))
        .andExpect(model().attribute("genres", sameInstance(GENRES)));

    verify(bookService, never()).create(any(BookForm.class));
    verify(authorService).findAll();
    verify(genreService).findAll();
  }

  @DisplayName("GET /books/{id}/edit должен показать форму с заполенными значениями")
  @Test
  void shouldRenderEditPage() throws Exception {
    var form = new BookForm("Война и мир", 1L, Set.of(100L, 101L));
    when(bookService.findById(BOOK_ID)).thenReturn(BOOK);
    when(bookMapper.toForm(BOOK)).thenReturn(form);
    stubFormOptions();

    mockMvc.perform(get("/books/{id}/edit", BOOK_ID))
        .andExpect(status().isOk())
        .andExpect(view().name("book/edit"))
        .andExpect(model().attribute("bookId", BOOK_ID))
        .andExpect(model().attribute("bookForm", sameInstance(form)))
        .andExpect(model().attribute("authors", sameInstance(AUTHORS)))
        .andExpect(model().attribute("genres", sameInstance(GENRES)));

    verify(bookService).findById(BOOK_ID);
    verify(bookMapper).toForm(BOOK);
    verify(authorService).findAll();
    verify(genreService).findAll();
  }

  @DisplayName("POST /books/{id} должен обновить книгу и перенаправить на ее страницу")
  @Test
  void shouldUpdateBookAndRedirectToDetails() throws Exception {
    var updatedBook = new BookDto(
        BOOK_ID,
        "Война и мир: новое издание",
        AUTHOR,
        GENRES
    );
    when(bookService.update(anyLong(), any(BookForm.class))).thenReturn(updatedBook);

    mockMvc.perform(post("/books/{id}", BOOK_ID)
            .param("title", "Война и мир: новое издание")
            .param("authorId", "1")
            .param("genreIds", "100", "101"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/books/" + BOOK_ID))
        .andExpect(flash().attribute("successMessageCode", "book.updated"));

    var formCaptor = ArgumentCaptor.forClass(BookForm.class);
    verify(bookService).update(eq(BOOK_ID), formCaptor.capture());

    BookForm submittedForm = formCaptor.getValue();
    assertThat(submittedForm.getTitle()).isEqualTo("Война и мир: новое издание");
    assertThat(submittedForm.getAuthorId()).isEqualTo(1L);
    assertThat(submittedForm.getGenreIds()).containsExactlyInAnyOrder(100L, 101L);

    verifyNoInteractions(authorService, genreService);
  }

  @DisplayName("POST /books/{id} должен заново показать страницу редактирования книги при непрохождении валидации")
  @Test
  void shouldRedisplayEditPageForInvalidForm() throws Exception {
    stubFormOptions();

    mockMvc.perform(post("/books/{id}", BOOK_ID)
            .param("title", " "))
        .andExpect(status().isOk())
        .andExpect(view().name("book/edit"))
        .andExpect(model().attribute("bookId", BOOK_ID))
        .andExpect(model().attributeHasFieldErrors(
            "bookForm",
            "title",
            "authorId",
            "genreIds"
        ))
        .andExpect(model().attribute("authors", sameInstance(AUTHORS)))
        .andExpect(model().attribute("genres", sameInstance(GENRES)));

    verify(bookService, never()).update(
        eq(BOOK_ID),
        any(BookForm.class)
    );
    verify(authorService).findAll();
    verify(genreService).findAll();
  }

  @DisplayName("POST /books/{id}/delete должен удалить книгу и перенаправить на страницу со списком книг")
  @Test
  void shouldDeleteBookAndRedirectToList() throws Exception {
    mockMvc.perform(post("/books/{id}/delete", BOOK_ID))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/books"))
        .andExpect(flash().attribute("successMessageCode", "book.deleted"));

    verify(bookService).deleteById(BOOK_ID);
  }

  @DisplayName("должен показывать страницу с ошибкой когда книга не существует")
  @Test
  void shouldRenderNotFoundPageWhenBookDoesNotExist() throws Exception {
    when(bookService.findById(BOOK_ID)).thenThrow(new BookNotFoundException(BOOK_ID));

    mockMvc.perform(get("/books/{id}", BOOK_ID).locale(Locale.ENGLISH))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error"))
        .andExpect(model().attribute("status", 404))
        .andExpect(model().attribute(
            "message",
            "Book with ID " + BOOK_ID + " was not found."
        ));

    verify(bookService).findById(BOOK_ID);
    verifyNoInteractions(commentService);
  }

  private void stubFormOptions() {
    when(authorService.findAll()).thenReturn(AUTHORS);
    when(genreService.findAll()).thenReturn(GENRES);
  }
}
