package ru.otus.hw.batch.config.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.otus.hw.batch.cache.BookCustomCache;
import ru.otus.hw.models.jpa.Book;
import ru.otus.hw.models.jpa.Comment;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.CommentMongo;
import ru.otus.hw.repositories.jpa.CommentJpaRepository;

class CommentStepConfigTest {

  private CommentJpaRepository repository;

  private BookCustomCache bookCache;

  private CommentStepConfig config;

  @BeforeEach
  void setUp() {
    repository = mock(CommentJpaRepository.class);
    bookCache = mock(BookCustomCache.class);
    config = new CommentStepConfig(
      repository,
      mock(MongoTemplate.class),
      mock(PlatformTransactionManager.class),
      mock(JobRepository.class),
      bookCache
    );
  }

  @Test
  void processorShouldMapCommentAndResolveBookFromCache() throws Exception {
    var sourceBook = new Book(15L, "Book", null, List.of());
    var source = new Comment(21L, "Insightful", sourceBook);
    var migratedBook = new BookMongo("book-mongo-id", "Book", null, List.of());
    when(bookCache.get(15L)).thenReturn(migratedBook);

    var result = config.commentProcessor().process(source);

    assertThat(result)
      .usingRecursiveComparison()
      .isEqualTo(new CommentMongo(null, "Insightful", migratedBook));
  }

  @Test
  void processorShouldHandleCommentWithoutBook() throws Exception {
    var source = new Comment(21L, "Detached", null);

    var result = config.commentProcessor().process(source);

    assertThat(result).isNotNull();
    assertThat(result.getText()).isEqualTo("Detached");
    assertThat(result.getBook()).isNull();
    verify(bookCache, never()).get(any(Long.class));
  }

  @Test
  void readerShouldUseTwentyItemPagesSortedByJpaId() throws Exception {
    var expected = new Comment(1L, "First", null);
    when(repository.findAll(any(Pageable.class)))
      .thenReturn(new PageImpl<>(List.of(expected)));
    var reader = config.commentReader();

    reader.open(new ExecutionContext());
    var actual = reader.read();
    reader.close();

    assertThat(actual).isSameAs(expected);
    var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    var idOrder = pageable.getValue().getSort().getOrderFor("id");
    assertThat(idOrder).isNotNull();
    assertThat(idOrder.getDirection()).isEqualTo(Direction.ASC);
  }

  @Test
  void stepShouldHaveStableNameAndAllowReruns() {
    var step = config.commentMigration(
      config.commentReader(), config.commentProcessor(), config.commentWriter());

    assertThat(step.getName()).isEqualTo("commentMigrationStep");
    assertThat(step.isAllowStartIfComplete()).isTrue();
  }
}
