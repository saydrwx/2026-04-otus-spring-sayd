package ru.otus.hw.batch.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.otus.hw.batch.cache.CustomCache;
import ru.otus.hw.models.jpa.Author;
import ru.otus.hw.models.mongo.AuthorMongo;

@ExtendWith(MockitoExtension.class)
class CustomMongoItemWriterTest {

  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private CustomCache<AuthorMongo> cache;

  private CustomMongoItemWriter<Author, AuthorMongo> writer;

  @BeforeEach
  void setUp() {
    writer = new CustomMongoItemWriter<>(mongoTemplate, cache);
  }

  @Test
  void shouldInsertEveryTargetBeforeCachingItByJpaId() {
    var firstSource = new Author(11L, "First");
    var secondSource = new Author(12L, "Second");
    var firstTarget = new AuthorMongo(null, "First");
    var secondTarget = new AuthorMongo(null, "Second");
    var chunk = new Chunk<>(List.of(
      Pair.of(firstSource, firstTarget),
      Pair.of(secondSource, secondTarget)
    ));

    writer.write(chunk);

    InOrder order = inOrder(mongoTemplate, cache);
    order.verify(mongoTemplate).insert(firstTarget);
    order.verify(cache).put(11L, firstTarget);
    order.verify(mongoTemplate).insert(secondTarget);
    order.verify(cache).put(12L, secondTarget);
    order.verifyNoMoreInteractions();
  }

  @Test
  void shouldNotPopulateCacheWhenMongoInsertFails() {
    var source = new Author(11L, "First");
    var target = new AuthorMongo(null, "First");
    var chunk = new Chunk<>(List.of(Pair.of(source, target)));
    var failure = new DataAccessResourceFailureException("Mongo is unavailable");
    org.mockito.Mockito.doThrow(failure).when(mongoTemplate).insert(target);

    assertThatThrownBy(() -> writer.write(chunk)).isSameAs(failure);

    verify(cache, never()).put(11L, target);
  }
}
