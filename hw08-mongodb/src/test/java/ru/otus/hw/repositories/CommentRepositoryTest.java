package ru.otus.hw.repositories;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import ru.otus.hw.TestDataPreparer;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Репозиторий на основе Spring Data MongoDB для работы с комментариями")
@DataMongoTest
@Import(TestDataPreparer.class)
public class CommentRepositoryTest {

  @Autowired
  private CommentRepository repository;

  @Autowired
  private TestDataPreparer testDataPreparer;

  private List<Book> dbBooks;

  private List<Comment> dbComments;

  @BeforeEach
  void setUp() {
    testDataPreparer.prepare();
    dbBooks = testDataPreparer.getDbBooks();
    dbComments = testDataPreparer.getDbComments();
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
    String bookId = "1";
    var actualComments = repository.findByBookId(bookId);
    var expectedComments = dbComments.stream()
      .filter(c -> c.getBook().getId().equals(bookId))
      .toList();

    assertThat(actualComments).containsExactlyElementsOf(expectedComments);
  }

  @DisplayName("должен сохранять новый комментарий")
  @Test
  void shouldSaveNewComment() {
    var expectedComment = new Comment(null, "comment text", dbBooks.get(1));
    var returnedComment = repository.save(expectedComment);
    assertThat(returnedComment).isNotNull()
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedComment);

    assertThat(repository.findById(returnedComment.getId()))
      .isPresent()
      .get()
      .isEqualTo(returnedComment);
  }

  @DisplayName("должен сохранять измененный комментарий")
  @Test
  void shouldSaveUpdatedComment() {
    var expectedComment = new Comment("2", "comment text", dbBooks.get(1));

    assertThat(repository.findById(expectedComment.getId()))
      .isPresent()
      .get()
      .isNotEqualTo(expectedComment);

    var returnedComment = repository.save(expectedComment);
    assertThat(returnedComment).isNotNull()
      .usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(expectedComment);

    assertThat(repository.findById(returnedComment.getId()))
      .isPresent()
      .get()
      .isEqualTo(returnedComment);
  }

  @DisplayName("должен удалять комментарий по id")
  @Test
  void shouldDeleteComment() {
    assertThat(repository.findById("1")).isPresent();
    repository.deleteById("1");
    assertThat(repository.findById("1")).isEmpty();
  }
}
