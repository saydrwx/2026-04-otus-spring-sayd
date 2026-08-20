package ru.otus.hw.batch.config.step;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.otus.hw.batch.cache.AuthorCustomCache;
import ru.otus.hw.batch.cache.BookCustomCache;
import ru.otus.hw.batch.cache.GenreCustomCache;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.CommentMongo;
import ru.otus.hw.models.mongo.GenreMongo;

@Configuration
@RequiredArgsConstructor
public class TaskletStepConfig {

  private final JobRepository jobRepository;

  private final PlatformTransactionManager platformTransactionManager;

  private final MongoTemplate mongoTemplate;

  private final AuthorCustomCache authorCache;

  private final GenreCustomCache genreCache;

  private final BookCustomCache bookCache;

  @Bean
  public Step cleanUpStep() {
    return new StepBuilder("cleanUpStep", jobRepository)
      .tasklet(cleanUpTasklet(), platformTransactionManager)
      .allowStartIfComplete(true)
      .build();
  }

  @Bean
  public CleanUpTasklet cleanUpTasklet() {
    return new CleanUpTasklet(mongoTemplate);
  }

  @AllArgsConstructor
  public class CleanUpTasklet implements Tasklet {

    private MongoTemplate mongoTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
      mongoTemplate.dropCollection(CommentMongo.class);
      mongoTemplate.dropCollection(BookMongo.class);
      mongoTemplate.dropCollection(GenreMongo.class);
      mongoTemplate.dropCollection(AuthorMongo.class);

      authorCache.clear();
      genreCache.clear();
      bookCache.clear();

      return RepeatStatus.FINISHED;
    }
  }
}
