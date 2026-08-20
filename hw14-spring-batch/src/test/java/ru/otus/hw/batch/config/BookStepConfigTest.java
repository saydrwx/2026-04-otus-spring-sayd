package ru.otus.hw.batch.config;

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
import ru.otus.hw.batch.cache.AuthorCustomCache;
import ru.otus.hw.batch.cache.BookCustomCache;
import ru.otus.hw.batch.cache.GenreCustomCache;
import ru.otus.hw.models.jpa.Author;
import ru.otus.hw.models.jpa.Book;
import ru.otus.hw.models.jpa.Genre;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.GenreMongo;
import ru.otus.hw.repositories.jpa.BookJpaRepository;

class BookStepConfigTest {

  private BookJpaRepository repository;

  private AuthorCustomCache authorCache;

  private GenreCustomCache genreCache;

  private BookStepConfig config;

  @BeforeEach
  void setUp() {
    repository = mock(BookJpaRepository.class);
    authorCache = mock(AuthorCustomCache.class);
    genreCache = mock(GenreCustomCache.class);
    config = new BookStepConfig(
      repository,
      mock(MongoTemplate.class),
      mock(PlatformTransactionManager.class),
      mock(JobRepository.class),
      authorCache,
      new BookCustomCache(),
      genreCache
    );
  }

  @Test
  void processorShouldMapBookAndResolveAuthorAndGenresFromCaches() throws Exception {
    var author = new Author(4L, "Author");
    var firstGenre = new Genre(7L, "First");
    var secondGenre = new Genre(8L, "Second");
    var source = new Book(12L, "A title", author, List.of(firstGenre, secondGenre));
    var migratedAuthor = new AuthorMongo("author-mongo-id", "Author");
    var migratedGenres = List.of(
      new GenreMongo("genre-7", "First"),
      new GenreMongo("genre-8", "Second")
    );
    when(authorCache.get(4L)).thenReturn(migratedAuthor);
    when(genreCache.get(List.of(7L, 8L))).thenReturn(migratedGenres);

    var result = config.bookProcessor().process(source);

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isSameAs(source);
    assertThat(result.getValue())
      .usingRecursiveComparison()
      .isEqualTo(new BookMongo(null, "A title", migratedAuthor, migratedGenres));
  }

  @Test
  void processorShouldHandleBookWithoutRelationships() throws Exception {
    var source = new Book(12L, "Orphaned", null, null);

    var result = config.bookProcessor().process(source);

    assertThat(result).isNotNull();
    assertThat(result.getValue().getTitle()).isEqualTo("Orphaned");
    assertThat(result.getValue().getAuthor()).isNull();
    assertThat(result.getValue().getGenres()).isEmpty();
    verify(authorCache, never()).get(any(Long.class));
    verify(genreCache, never()).get(any(List.class));
  }

  @Test
  void readerShouldUseTwentyItemPagesSortedByJpaId() throws Exception {
    var expected = new Book(1L, "First", null, List.of());
    when(repository.findAll(any(Pageable.class)))
      .thenReturn(new PageImpl<>(List.of(expected)));
    var reader = config.bookReader();

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
    var step = config.bookMigration(
      config.bookReader(), config.bookProcessor(), config.bookWriter());

    assertThat(step.getName()).isEqualTo("bookMigrationStep");
    assertThat(step.isAllowStartIfComplete()).isTrue();
  }
}
