package ru.otus.hw.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.mapper.AuthorMapper;
import ru.otus.hw.mapper.BookMapper;
import ru.otus.hw.mapper.CommentMapper;
import ru.otus.hw.mapper.GenreMapper;
import ru.otus.hw.repository.UserRepository;
import ru.otus.hw.security.SecurityConfiguration;
import ru.otus.hw.service.AuthorService;
import ru.otus.hw.service.BookService;
import ru.otus.hw.service.CommentService;
import ru.otus.hw.service.GenreService;
import ru.otus.hw.service.UserService;

@DisplayName("Правила авторизации контроллеров")
@WebMvcTest({
    AuthenticationController.class,
    AuthorController.class,
    BookController.class,
    CommentController.class,
    GenreController.class,
    HomeController.class
})
@Import(SecurityConfiguration.class)
class AuthorizationTest {

  private static final long AUTHOR_ID = 1L;

  private static final long GENRE_ID = 2L;

  private static final long BOOK_ID = 3L;

  private static final long COMMENT_ID = 4L;

  private static final String USER_NAME = "test-user";

  private static final AuthorDto AUTHOR = new AuthorDto(AUTHOR_ID, "Test Author");

  private static final GenreDto GENRE = new GenreDto(GENRE_ID, "Test Genre");

  private static final BookDto BOOK = new BookDto(
      BOOK_ID,
      "Test Book",
      AUTHOR,
      List.of(GENRE)
  );

  private static final CommentDto COMMENT = new CommentDto(
      COMMENT_ID,
      "Test comment",
      BOOK_ID
  );

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private RequestMappingHandlerMapping handlerMapping;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private AuthorService authorService;

  @MockitoBean
  private AuthorMapper authorMapper;

  @MockitoBean
  private GenreService genreService;

  @MockitoBean
  private GenreMapper genreMapper;

  @MockitoBean
  private BookService bookService;

  @MockitoBean
  private BookMapper bookMapper;

  @MockitoBean
  private CommentService commentService;

  @MockitoBean
  private CommentMapper commentMapper;

  @BeforeEach
  void setUpControllerResponses() {
    when(authorService.findAll()).thenReturn(List.of(AUTHOR));
    when(authorService.findById(AUTHOR_ID)).thenReturn(AUTHOR);
    when(authorMapper.toForm(AUTHOR)).thenReturn(new AuthorForm(AUTHOR.fullName()));
    when(genreService.findAll()).thenReturn(List.of(GENRE));
    when(genreService.findById(GENRE_ID)).thenReturn(GENRE);
    when(genreMapper.toForm(GENRE)).thenReturn(new GenreForm(GENRE.name()));
    when(bookService.findAll()).thenReturn(List.of(BOOK));
    when(bookService.findById(BOOK_ID)).thenReturn(BOOK);
    when(bookService.create(any(BookForm.class))).thenReturn(BOOK);
    when(bookMapper.toForm(BOOK)).thenReturn(bookForm());
    when(commentService.findAllByBookId(BOOK_ID)).thenReturn(List.of(COMMENT));
    when(commentService.findById(COMMENT_ID)).thenReturn(COMMENT);
    when(commentMapper.toForm(COMMENT)).thenReturn(new CommentForm(COMMENT.text()));
  }

  @DisplayName("публичный endpoint доступен анонимному пользователю")
  @ParameterizedTest(name = "{0}")
  @MethodSource("publicEndpoints")
  void shouldAllowAnonymousAccessToPublicEndpoints(
      String description,
      MockHttpServletRequestBuilder request,
      int expectedStatus
  ) throws Exception {
    mockMvc.perform(request)
        .andExpect(status().is(expectedStatus));
  }

  @DisplayName("публичный endpoint доступен авторизованному пользователю")
  @ParameterizedTest(name = "{0}")
  @MethodSource("publicEndpoints")
  void shouldAllowAuthenticatedAccessToPublicEndpoints(
      String description,
      MockHttpServletRequestBuilder request,
      int expectedStatus
  ) throws Exception {
    mockMvc.perform(request.with(user(USER_NAME)))
        .andExpect(status().is(expectedStatus));
  }

  @DisplayName(
      "защищенный endpoint перенаправляет анонимного пользователя"
  )
  @ParameterizedTest(name = "{0}")
  @MethodSource({
      "homeEndpoints",
      "authorEndpoints",
      "genreEndpoints",
      "bookEndpoints",
      "commentEndpoints"
  })
  void shouldRedirectAnonymousUserFromProtectedEndpoint(
      String description,
      RequestBuilder request
  ) throws Exception {
    mockMvc.perform(request)
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));

    verifyNoControllerInteractions();
  }

  @DisplayName("защищенный endpoint доступен авторизованному пользователю")
  @ParameterizedTest(name = "{0}")
  @MethodSource({
      "homeEndpoints",
      "authorEndpoints",
      "genreEndpoints",
      "bookEndpoints",
      "commentEndpoints"
  })
  void shouldAllowAuthenticatedAccessToProtectedEndpoint(
      String description,
      MockHttpServletRequestBuilder request,
      int expectedStatus
  ) throws Exception {
    mockMvc.perform(request.with(user(USER_NAME)))
        .andExpect(status().is(expectedStatus));
  }

  @DisplayName(
      "набор тестовых данных содержит каждый endpoint контроллеров"
  )
  @Test
  void shouldIncludeEveryControllerEndpoint() {
    Set<String> testedEndpoints = allEndpoints()
        .map(arguments -> arguments.get()[0].toString())
        .collect(Collectors.toSet());

    Set<String> controllerEndpoints = handlerMapping.getHandlerMethods()
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue().getBeanType().getPackageName()
            .equals("ru.otus.hw.controller"))
        .flatMap(entry -> entry.getKey().getMethodsCondition().getMethods().stream()
            .flatMap(method -> entry.getKey().getPatternValues().stream()
                .map(path -> method.name() + " " + path)))
        .collect(Collectors.toSet());

    assertThat(testedEndpoints).containsExactlyInAnyOrderElementsOf(controllerEndpoints);
  }

  private static Stream<Arguments> allEndpoints() {
    return Stream.of(
        publicEndpoints(),
        homeEndpoints(),
        authorEndpoints(),
        genreEndpoints(),
        bookEndpoints(),
        commentEndpoints()
    ).flatMap(stream -> stream);
  }

  private static Stream<Arguments> publicEndpoints() {
    return Stream.of(
        endpoint("GET /login", get("/login"), 200),
        endpoint("GET /register", get("/register"), 200),
        endpoint("POST /register", registrationRequest(), 302)
    );
  }

  private static Stream<Arguments> homeEndpoints() {
    return Stream.of(endpoint("GET /", get("/"), 200));
  }

  private static Stream<Arguments> authorEndpoints() {
    return Stream.of(
        endpoint("GET /authors", get("/authors"), 200),
        endpoint("GET /authors/new", get("/authors/new"), 200),
        endpoint("POST /authors", authorRequest("/authors"), 302),
        endpoint("GET /authors/{id}/edit", get("/authors/{id}/edit", AUTHOR_ID), 200),
        endpoint("POST /authors/{id}", authorRequest("/authors/{id}", AUTHOR_ID), 302),
        endpoint("POST /authors/{id}/delete", deleteRequest("/authors/{id}/delete", AUTHOR_ID), 302)
    );
  }

  private static Stream<Arguments> genreEndpoints() {
    return Stream.of(
        endpoint("GET /genres", get("/genres"), 200),
        endpoint("GET /genres/new", get("/genres/new"), 200),
        endpoint("POST /genres", genreRequest("/genres"), 302),
        endpoint("GET /genres/{id}/edit", get("/genres/{id}/edit", GENRE_ID), 200),
        endpoint("POST /genres/{id}", genreRequest("/genres/{id}", GENRE_ID), 302),
        endpoint("POST /genres/{id}/delete", deleteRequest("/genres/{id}/delete", GENRE_ID), 302)
    );
  }

  private static Stream<Arguments> bookEndpoints() {
    return Stream.of(
        endpoint("GET /books", get("/books"), 200),
        endpoint("GET /books/{id}", get("/books/{id}", BOOK_ID), 200),
        endpoint("GET /books/new", get("/books/new"), 200),
        endpoint("POST /books", bookRequest("/books"), 302),
        endpoint("GET /books/{id}/edit", get("/books/{id}/edit", BOOK_ID), 200),
        endpoint("POST /books/{id}", bookRequest("/books/{id}", BOOK_ID), 302),
        endpoint("POST /books/{id}/delete", deleteRequest("/books/{id}/delete", BOOK_ID), 302)
    );
  }

  private static Stream<Arguments> commentEndpoints() {
    return Stream.of(
        endpoint("GET /books/{bookId}/comments/new", get("/books/{bookId}/comments/new", BOOK_ID), 200),
        endpoint("POST /books/{bookId}/comments", commentRequest("/books/{bookId}/comments", BOOK_ID), 302),
        endpoint("GET /books/{bookId}/comments/{commentId}/edit",
            get(commentPath("/edit"), BOOK_ID, COMMENT_ID), 200),
        endpoint("POST /books/{bookId}/comments/{commentId}",
            commentRequest(commentPath(""), BOOK_ID, COMMENT_ID), 302),
        endpoint("POST /books/{bookId}/comments/{commentId}/delete",
            deleteRequest(commentPath("/delete"), BOOK_ID, COMMENT_ID), 302)
    );
  }

  private static Arguments endpoint(
      String description,
      MockHttpServletRequestBuilder request,
      int expectedStatus
  ) {
    return Arguments.of(description, request, expectedStatus);
  }

  private static MockHttpServletRequestBuilder registrationRequest() {
    return post("/register")
        .with(csrf())
        .param("name", USER_NAME)
        .param("password", "password");
  }

  private static MockHttpServletRequestBuilder authorRequest(String path, Object... uriVariables) {
    return post(path, uriVariables)
        .with(csrf())
        .param("fullName", AUTHOR.fullName());
  }

  private static MockHttpServletRequestBuilder genreRequest(String path, Object... uriVariables) {
    return post(path, uriVariables)
        .with(csrf())
        .param("name", GENRE.name());
  }

  private static MockHttpServletRequestBuilder bookRequest(String path, Object... uriVariables) {
    return post(path, uriVariables)
        .with(csrf())
        .param("title", BOOK.title())
        .param("authorId", String.valueOf(AUTHOR_ID))
        .param("genreIds", String.valueOf(GENRE_ID));
  }

  private static MockHttpServletRequestBuilder commentRequest(String path, Object... uriVariables) {
    return post(path, uriVariables)
        .with(csrf())
        .param("text", COMMENT.text());
  }

  private static MockHttpServletRequestBuilder deleteRequest(String path, Object... uriVariables) {
    return post(path, uriVariables).with(csrf());
  }

  private static String commentPath(String suffix) {
    return "/books/{bookId}/comments/{commentId}" + suffix;
  }

  private static BookForm bookForm() {
    return new BookForm(BOOK.title(), AUTHOR_ID, Set.of(GENRE_ID));
  }

  private void verifyNoControllerInteractions() {
    verifyNoInteractions(
        userService,
        authorService,
        authorMapper,
        genreService,
        genreMapper,
        bookService,
        bookMapper,
        commentService,
        commentMapper
    );
  }
}
