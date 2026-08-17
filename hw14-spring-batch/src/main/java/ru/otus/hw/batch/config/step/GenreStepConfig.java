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
import ru.otus.hw.batch.cache.GenreCustomCache;
import ru.otus.hw.batch.writer.CustomMongoItemWriter;
import ru.otus.hw.models.jpa.Genre;
import ru.otus.hw.models.mongo.GenreMongo;
import ru.otus.hw.repositories.jpa.GenreJpaRepository;

@Configuration
@RequiredArgsConstructor
public class GenreStepConfig {

  private final GenreJpaRepository genreRepository;

  private final MongoTemplate mongoTemplate;

  private final PlatformTransactionManager platformTransactionManager;

  private final JobRepository jobRepository;

  private final GenreCustomCache cache;

  @Bean
  public Step genreMigration(
    RepositoryItemReader<Genre> genreReader,
    ItemProcessor<Genre, Pair<Genre, GenreMongo>> genreProcessor,
    CustomMongoItemWriter<Genre, GenreMongo> genreWriter
  ) {
    return new StepBuilder("genreMigrationStep", jobRepository)
      .<Genre, Pair<Genre, GenreMongo>>chunk(5, platformTransactionManager)
      .reader(genreReader)
      .processor(genreProcessor)
      .writer(genreWriter)
      .allowStartIfComplete(true)
      .build();
  }

  @Bean
  public RepositoryItemReader<Genre> genreReader() {
    return new RepositoryItemReaderBuilder<Genre>()
      .name("genreReader")
      .repository(genreRepository)
      .methodName("findAll")
      .pageSize(20)
      .sorts(Map.of("id", Direction.ASC))
      .build();
  }

  @Bean
  public ItemProcessor<Genre, Pair<Genre, GenreMongo>> genreProcessor() {
    return item -> Pair.of(item, new GenreMongo(null, item.getName()));
  }

  @Bean
  public CustomMongoItemWriter<Genre, GenreMongo> genreWriter() {
    return new CustomMongoItemWriter<>(mongoTemplate, cache);
  }
}
