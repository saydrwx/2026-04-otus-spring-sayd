package ru.otus.hw.batch.config.step;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.otus.hw.batch.cache.BookCustomCache;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.CommentMongo;
import ru.otus.hw.models.jpa.Comment;
import ru.otus.hw.repositories.jpa.CommentJpaRepository;

@Configuration
@RequiredArgsConstructor
public class CommentStepConfig {

  private final CommentJpaRepository commentRepository;

  private final MongoTemplate mongoTemplate;

  private final PlatformTransactionManager platformTransactionManager;

  private final JobRepository jobRepository;

  private final BookCustomCache bookCache;

  @Bean
  public Step commentMigration(
    RepositoryItemReader<Comment> commentReader,
    ItemProcessor<Comment, CommentMongo> commentProcessor,
    MongoItemWriter<CommentMongo> commentWriter
  ) {
    return new StepBuilder("commentMigrationStep", jobRepository)
      .<Comment, CommentMongo>chunk(5, platformTransactionManager)
      .reader(commentReader)
      .processor(commentProcessor)
      .writer(commentWriter)
      .allowStartIfComplete(true)
      .build();
  }

  @Bean
  public RepositoryItemReader<Comment> commentReader() {
    return new RepositoryItemReaderBuilder<Comment>()
      .name("commentReader")
      .repository(commentRepository)
      .methodName("findAll")
      .pageSize(20)
      .sorts(Map.of("id", Direction.ASC))
      .build();
  }

  @Bean
  public ItemProcessor<Comment, CommentMongo> commentProcessor() {
    return item -> {
      Long bookId = item.getBook() != null ? item.getBook().getId() : null;
      BookMongo bookMongo = bookId != null ? bookCache.get(bookId) : null;
      return new CommentMongo(null, item.getText(), bookMongo);
    };
  }

  @Bean
  public MongoItemWriter<CommentMongo> commentWriter() {
    return new MongoItemWriterBuilder<CommentMongo>()
      .collection("comments")
      .template(mongoTemplate)
      .build();
  }
}
