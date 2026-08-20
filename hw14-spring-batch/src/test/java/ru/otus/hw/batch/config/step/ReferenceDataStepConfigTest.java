package ru.otus.hw.batch.config.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.otus.hw.batch.cache.AuthorCustomCache;
import ru.otus.hw.batch.cache.GenreCustomCache;
import ru.otus.hw.models.jpa.Author;
import ru.otus.hw.models.jpa.Genre;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.models.mongo.GenreMongo;
import ru.otus.hw.repositories.jpa.AuthorJpaRepository;
import ru.otus.hw.repositories.jpa.GenreJpaRepository;

class ReferenceDataStepConfigTest {

  @Test
  void authorProcessorShouldPreserveSourceAndMapBusinessFields() throws Exception {
    var config = authorConfig(mock(AuthorJpaRepository.class));
    var source = new Author(7L, "Ursula Le Guin");

    var result = config.authorProcessor().process(source);

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isSameAs(source);
    assertThat(result.getValue())
      .usingRecursiveComparison()
      .isEqualTo(new AuthorMongo(null, "Ursula Le Guin"));
  }

  @Test
  void genreProcessorShouldPreserveSourceAndMapBusinessFields() throws Exception {
    var config = genreConfig(mock(GenreJpaRepository.class));
    var source = new Genre(9L, "Science fiction");

    var result = config.genreProcessor().process(source);

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isSameAs(source);
    assertThat(result.getValue())
      .usingRecursiveComparison()
      .isEqualTo(new GenreMongo(null, "Science fiction"));
  }

  @Test
  void authorReaderShouldRequestFirstPageSortedByJpaId() throws Exception {
    var repository = mock(AuthorJpaRepository.class);
    var expected = new Author(1L, "First");
    when(repository.findAll(any(Pageable.class)))
      .thenReturn(new PageImpl<>(List.of(expected)));
    RepositoryItemReader<Author> reader = authorConfig(repository).authorReader();

    reader.open(new ExecutionContext());
    var actual = reader.read();
    reader.close();

    assertThat(actual).isSameAs(expected);
    var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(repository).findAll(pageable.capture());
    assertPageConfiguration(pageable.getValue());
  }

  @Test
  void genreReaderShouldRequestFirstPageSortedByJpaId() throws Exception {
    var repository = mock(GenreJpaRepository.class);
    var expected = new Genre(1L, "First");
    when(repository.findAll(any(Pageable.class)))
      .thenReturn(new PageImpl<>(List.of(expected)));
    RepositoryItemReader<Genre> reader = genreConfig(repository).genreReader();

    reader.open(new ExecutionContext());
    var actual = reader.read();
    reader.close();

    assertThat(actual).isSameAs(expected);
    var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(repository).findAll(pageable.capture());
    assertPageConfiguration(pageable.getValue());
  }

  @Test
  void authorAndGenreStepsShouldBeRestartableAfterCompletion() {
    var authorConfig = authorConfig(mock(AuthorJpaRepository.class));
    var genreConfig = genreConfig(mock(GenreJpaRepository.class));

    var authorStep = authorConfig.authorMigration(
      authorConfig.authorReader(), authorConfig.authorProcessor(), authorConfig.authorWriter());
    var genreStep = genreConfig.genreMigration(
      genreConfig.genreReader(), genreConfig.genreProcessor(), genreConfig.genreWriter());

    assertThat(authorStep.getName()).isEqualTo("authorMigrationStep");
    assertThat(authorStep.isAllowStartIfComplete()).isTrue();
    assertThat(genreStep.getName()).isEqualTo("genreMigrationStep");
    assertThat(genreStep.isAllowStartIfComplete()).isTrue();
  }

  private static AuthorStepConfig authorConfig(AuthorJpaRepository repository) {
    return new AuthorStepConfig(
      repository,
      mock(MongoTemplate.class),
      mock(PlatformTransactionManager.class),
      mock(JobRepository.class),
      new AuthorCustomCache()
    );
  }

  private static GenreStepConfig genreConfig(GenreJpaRepository repository) {
    return new GenreStepConfig(
      repository,
      mock(MongoTemplate.class),
      mock(PlatformTransactionManager.class),
      mock(JobRepository.class),
      new GenreCustomCache()
    );
  }

  private static void assertPageConfiguration(Pageable pageable) {
    assertThat(pageable.getPageNumber()).isZero();
    assertThat(pageable.getPageSize()).isEqualTo(20);
    var idOrder = pageable.getSort().getOrderFor("id");
    assertThat(idOrder).isNotNull();
    assertThat(idOrder.getDirection()).isEqualTo(Direction.ASC);
  }
}
