package ru.otus.hw.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.acls.domain.BasePermission;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.mapper.AuthorMapper;
import ru.otus.hw.mapper.BookMapper;
import ru.otus.hw.mapper.CommentMapper;
import ru.otus.hw.mapper.GenreMapper;
import ru.otus.hw.model.Author;
import ru.otus.hw.model.Book;
import ru.otus.hw.model.Comment;
import ru.otus.hw.model.Genre;
import ru.otus.hw.repository.AuthorRepository;
import ru.otus.hw.repository.BookRepository;
import ru.otus.hw.repository.CommentRepository;
import ru.otus.hw.repository.GenreRepository;

@DisplayName("ACL в CRUD сервисах библиотеки")
class AclLifecycleServiceTest {

  @Test
  @DisplayName("создание автора приводит к созданию READ/WRITE/DELETE ACL")
  void authorCreateShouldCreatePermissions() {
    AuthorRepository repository = mock(AuthorRepository.class);
    AuthorMapper mapper = mock(AuthorMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    AuthorServiceImpl service = new AuthorServiceImpl(repository, mapper, aclService);

    AuthorForm form = new AuthorForm("New Author");
    Author transientAuthor = new Author("New Author");
    Author savedAuthor = new Author(10L, "New Author");

    when(mapper.toEntity(form)).thenReturn(transientAuthor);
    when(repository.save(transientAuthor)).thenReturn(savedAuthor);

    service.create(form);

    verify(aclService).createPermission(
        savedAuthor,
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE
    );
  }

  @Test
  @DisplayName("создание жанра приводит к созданию READ/WRITE/DELETE ACL")
  void genreCreateShouldCreatePermissions() {
    GenreRepository repository = mock(GenreRepository.class);
    GenreMapper mapper = mock(GenreMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    GenreServiceImpl service = new GenreServiceImpl(repository, mapper, aclService);

    GenreForm form = new GenreForm("New Genre");
    Genre transientGenre = new Genre("New Genre");
    Genre savedGenre = new Genre(10L, "New Genre");

    when(mapper.toEntity(form)).thenReturn(transientGenre);
    when(repository.save(transientGenre)).thenReturn(savedGenre);

    service.create(form);

    verify(aclService).createPermission(
        savedGenre,
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE
    );
  }

  @Test
  @DisplayName("создание книги приводит к созданию READ/WRITE/DELETE ACL")
  void bookCreateShouldCreatePermissions() {
    BookRepository bookRepository = mock(BookRepository.class);
    AuthorRepository authorRepository = mock(AuthorRepository.class);
    GenreRepository genreRepository = mock(GenreRepository.class);
    BookMapper mapper = mock(BookMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    BookServiceImpl service = new BookServiceImpl(
        bookRepository,
        authorRepository,
        genreRepository,
        mapper,
        aclService
    );

    Author author = new Author(1L, "Author");
    Genre genre1 = new Genre(1L, "Genre 1");
    Genre genre2 = new Genre(2L, "Genre 2");
    BookForm form = new BookForm("New Book", 1L, Set.of(1L, 2L));
    Book transientBook = new Book("New Book", author, List.of(genre1, genre2));
    Book savedBook = new Book(10L, "New Book", author, Set.of(genre1, genre2));

    when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
    when(genreRepository.findAllById(form.getGenreIds())).thenReturn(List.of(genre1, genre2));
    when(mapper.toEntity(form, author, List.of(genre1, genre2))).thenReturn(transientBook);
    when(bookRepository.save(transientBook)).thenReturn(savedBook);

    service.create(form);

    verify(aclService).createPermission(
        savedBook,
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE
    );
  }

  @Test
  @DisplayName("создание комментария приводит к созданию READ/WRITE/DELETE ACL")
  void commentCreateShouldCreatePermissions() {
    CommentRepository commentRepository = mock(CommentRepository.class);
    BookRepository bookRepository = mock(BookRepository.class);
    CommentMapper mapper = mock(CommentMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    CommentServiceImpl service = new CommentServiceImpl(
        commentRepository,
        bookRepository,
        mapper,
        aclService
    );

    Book book = mock(Book.class);
    CommentForm form = new CommentForm("New comment");
    Comment transientComment = new Comment("New comment", book);
    Comment savedComment = new Comment(10L, "New comment", book);

    when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
    when(mapper.toEntity(form, book)).thenReturn(transientComment);
    when(commentRepository.save(transientComment)).thenReturn(savedComment);

    service.create(1L, form);

    verify(aclService).createPermission(
        savedComment,
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE
    );
  }

  @Test
  @DisplayName("удаление автора удаляет ACL, а затем сущность")
  void authorDeleteShouldDeletePermissionsAndEntityInOrder() {
    AuthorRepository repository = mock(AuthorRepository.class);
    AuthorMapper mapper = mock(AuthorMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    AuthorServiceImpl service = new AuthorServiceImpl(repository, mapper, aclService);
    Author author = new Author(1L, "Author");

    when(repository.findById(1L)).thenReturn(Optional.of(author));

    service.deleteById(1L);

    InOrder inOrder = inOrder(repository, aclService);
    inOrder.verify(repository).findById(1L);
    inOrder.verify(aclService).deletePermissions(1L, Author.class);
    inOrder.verify(repository).delete(author);
  }

  @Test
  @DisplayName("удаление жанра удаляет ACL, а затем сущность")
  void genreDeleteShouldDeletePermissions() {
    GenreRepository repository = mock(GenreRepository.class);
    GenreMapper mapper = mock(GenreMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    GenreServiceImpl service = new GenreServiceImpl(repository, mapper, aclService);
    Genre genre = new Genre(1L, "Genre");

    when(repository.findById(1L)).thenReturn(Optional.of(genre));

    service.deleteById(1L);

    InOrder inOrder = inOrder(repository, aclService);
    inOrder.verify(repository).findById(1L);
    inOrder.verify(aclService).deletePermissions(1L, Genre.class);
    inOrder.verify(repository).delete(genre);
  }

  @Test
  @DisplayName("удаление книги удаляет ACL, а затем сущность")
  void bookDeleteShouldDeletePermissions() {
    BookRepository bookRepository = mock(BookRepository.class);
    AuthorRepository authorRepository = mock(AuthorRepository.class);
    GenreRepository genreRepository = mock(GenreRepository.class);
    BookMapper mapper = mock(BookMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    BookServiceImpl service = new BookServiceImpl(
        bookRepository,
        authorRepository,
        genreRepository,
        mapper,
        aclService
    );
    Book book = mock(Book.class);

    when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

    service.deleteById(1L);

    InOrder inOrder = inOrder(bookRepository, aclService);
    inOrder.verify(bookRepository).findById(1L);
    inOrder.verify(aclService).deletePermissions(1L, Book.class);
    inOrder.verify(bookRepository).delete(book);
  }

  @Test
  @DisplayName("удаление комментария удаляет ACL, а затем сущность")
  void commentDeleteShouldDeletePermissions() {
    CommentRepository commentRepository = mock(CommentRepository.class);
    BookRepository bookRepository = mock(BookRepository.class);
    CommentMapper mapper = mock(CommentMapper.class);
    AclServiceWrapperService aclService = mock(AclServiceWrapperService.class);
    CommentServiceImpl service = new CommentServiceImpl(
        commentRepository,
        bookRepository,
        mapper,
        aclService
    );
    Comment comment = mock(Comment.class);

    when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

    service.deleteById(1L);

    InOrder inOrder = inOrder(commentRepository, aclService);
    inOrder.verify(commentRepository).findById(1L);
    inOrder.verify(aclService).deletePermissions(1L, Comment.class);
    inOrder.verify(commentRepository).delete(comment);
  }
}
