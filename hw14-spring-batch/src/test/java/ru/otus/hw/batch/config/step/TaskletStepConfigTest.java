package ru.otus.hw.batch.config.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.otus.hw.batch.cache.AuthorCustomCache;
import ru.otus.hw.batch.cache.BookCustomCache;
import ru.otus.hw.batch.cache.GenreCustomCache;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.CommentMongo;
import ru.otus.hw.models.mongo.GenreMongo;

class TaskletStepConfigTest {

  private MongoTemplate mongoTemplate;

  private AuthorCustomCache authorCache;

  private GenreCustomCache genreCache;

  private BookCustomCache bookCache;

  private TaskletStepConfig config;

  @BeforeEach
  void setUp() {
    mongoTemplate = mock(MongoTemplate.class);
    authorCache = mock(AuthorCustomCache.class);
    genreCache = mock(GenreCustomCache.class);
    bookCache = mock(BookCustomCache.class);
    config = new TaskletStepConfig(
      mock(JobRepository.class),
      mock(PlatformTransactionManager.class),
      mongoTemplate,
      authorCache,
      genreCache,
      bookCache
    );
  }

  @Test
  void taskletShouldDropDependentCollectionsFirstAndClearEveryCache() {
    var result = config.cleanUpTasklet().execute(null, null);

    assertThat(result).isEqualTo(RepeatStatus.FINISHED);
    InOrder collectionOrder = inOrder(mongoTemplate);
    collectionOrder.verify(mongoTemplate).dropCollection(CommentMongo.class);
    collectionOrder.verify(mongoTemplate).dropCollection(BookMongo.class);
    collectionOrder.verify(mongoTemplate).dropCollection(GenreMongo.class);
    collectionOrder.verify(mongoTemplate).dropCollection(AuthorMongo.class);
    collectionOrder.verifyNoMoreInteractions();
    verify(authorCache).clear();
    verify(genreCache).clear();
    verify(bookCache).clear();
  }

  @Test
  void taskletShouldPropagateMongoFailureWithoutClearingCaches() {
    var failure = new DataAccessResourceFailureException("Cannot drop collection");
    org.mockito.Mockito.doThrow(failure)
      .when(mongoTemplate).dropCollection(CommentMongo.class);

    assertThatThrownBy(() -> config.cleanUpTasklet().execute(null, null))
      .isSameAs(failure);

    verify(mongoTemplate, never()).dropCollection(BookMongo.class);
    verify(authorCache, never()).clear();
    verify(genreCache, never()).clear();
    verify(bookCache, never()).clear();
  }

  @Test
  void cleanupStepShouldHaveStableNameAndAllowReruns() {
    var step = config.cleanUpStep();

    assertThat(step.getName()).isEqualTo("cleanUpStep");
    assertThat(step.isAllowStartIfComplete()).isTrue();
  }
}
