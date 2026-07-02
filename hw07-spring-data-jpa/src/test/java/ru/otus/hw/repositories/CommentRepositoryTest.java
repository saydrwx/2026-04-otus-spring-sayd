package ru.otus.hw.repositories;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Репозиторий на основе JPA для работы с комментариями")
@DataJpaTest
public class CommentRepositoryTest {

  @Autowired
  private CommentRepository repository;

  @Autowired
  private TestEntityManager em;

  private List<Book> dbBooks;

  private List<Comment> dbComments;

  @BeforeEach
  void setUp() {
    dbBooks = getDbBooks(getDbAuthors(), getDbGenres());
    dbComments = getDbComments(dbBooks);
  }

  @DisplayName("должен загружать комментарий по id")
  @Test
  void shouldReturnCorrectCommentById() {
    var expectedComment = dbComments.get(1);
    var actualComment = repository.findById(expectedComment.getId());
    assertThat(actualComment).isPresent().get().isEqualTo(expectedComment);
  }

  @DisplayName("должен загружать список всех комментариев для книги по id")
  @Test
  void shouldReturnCorrectCommentsList() {
    long bookId = 1L;
    var actualComments = repository.findByBookId(bookId);
    var expectedComments = dbComments.stream()
      .filter(c -> c.getBook().getId() == bookId)
      .toList();

    assertThat(actualComments).containsExactlyElementsOf(expectedComments);
  }

  @DisplayName("должен сохранять новый комментарий")
  @Test
  void shouldSaveNewComment() {
    var expectedComment = new Comment(0, "comment text", dbBooks.get(1));
    var returnedComment = repository.save(expectedComment);
    assertThat(returnedComment).isNotNull()
      .matches(comment -> comment.getId() > 0)
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedComment);

    assertThat(Optional.ofNullable(em.find(Comment.class, returnedComment.getId())))
      .isPresent()
      .get()
      .isEqualTo(returnedComment);
  }

  @DisplayName("должен сохранять измененный комментарий")
  @Test
  void shouldSaveUpdatedComment() {
    var expectedComment = new Comment(2L, "comment text", dbBooks.get(1));

    assertThat(repository.findById(expectedComment.getId()))
      .isPresent()
      .get()
      .isNotEqualTo(expectedComment);

    var returnedComment = repository.save(expectedComment);
    assertThat(returnedComment).isNotNull()
      .matches(book -> book.getId() > 0)
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedComment);

    assertThat(Optional.ofNullable(em.find(Comment.class, returnedComment.getId())))
      .isPresent()
      .get()
      .isEqualTo(returnedComment);
  }

  @DisplayName("должен удалять комментарий по id")
  @Test
  void shouldDeleteComment() {
    assertThat(repository.findById(1L)).isPresent();
    repository.deleteById(1L);
    assertThat(repository.findById(1L)).isEmpty();
  }

  private static List<Author> getDbAuthors() {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Author(id, "Author_" + id))
      .toList();
  }

  private static List<Genre> getDbGenres() {
    return IntStream.range(1, 7).boxed()
      .map(id -> new Genre(id, "Genre_" + id))
      .toList();
  }

  private static List<Book> getDbBooks(List<Author> dbAuthors, List<Genre> dbGenres) {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Book(id,
        "BookTitle_" + id,
        dbAuthors.get(id - 1),
        dbGenres.subList((id - 1) * 2, (id - 1) * 2 + 2)
      ))
      .toList();
  }

  public static List<Comment> getDbComments(List<Book> dbBooks) {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Comment(id,
        "BookTitle_" + id + " comment",
        dbBooks.get(id - 1)
      ))
      .toList();
  }
}
