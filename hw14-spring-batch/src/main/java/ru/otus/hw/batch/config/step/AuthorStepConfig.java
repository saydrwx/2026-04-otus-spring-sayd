package ru.otus.hw.batch.config.step;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.otus.hw.batch.cache.AuthorCustomCache;
import ru.otus.hw.batch.writer.CustomMongoItemWriter;
import ru.otus.hw.models.jpa.Author;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.repositories.jpa.AuthorJpaRepository;

@Configuration
@RequiredArgsConstructor
public class AuthorStepConfig {

  private final AuthorJpaRepository authorRepository;

  private final MongoTemplate mongoTemplate;

  private final PlatformTransactionManager platformTransactionManager;

  private final JobRepository jobRepository;

  private final AuthorCustomCache cache;

  @Bean
  public Step authorMigration(
    RepositoryItemReader<Author> authorReader,
    ItemProcessor<Author, Pair<Author, AuthorMongo>> authorProcessor,
    CustomMongoItemWriter<Author, AuthorMongo> authorWriter
  ) {
    return new StepBuilder("authorMigrationStep", jobRepository)
      .<Author, Pair<Author, AuthorMongo>>chunk(5, platformTransactionManager)
      .reader(authorReader)
      .processor(authorProcessor)
      .writer(authorWriter)
      .allowStartIfComplete(true)
      .build();
  }

  @Bean
  public RepositoryItemReader<Author> authorReader() {
    return new RepositoryItemReaderBuilder<Author>()
      .name("authorReader")
      .repository(authorRepository)
      .methodName("findAll")
      .pageSize(20)
      .sorts(Map.of("id", Direction.ASC))
      .build();
  }

  @Bean
  public ItemProcessor<Author, Pair<Author, AuthorMongo>> authorProcessor() {
    return item -> Pair.of(item, new AuthorMongo(null, item.getFullName()));
  }

  @Bean
  public CustomMongoItemWriter<Author, AuthorMongo> authorWriter() {
    return new CustomMongoItemWriter<>(mongoTemplate, cache);
  }
}
