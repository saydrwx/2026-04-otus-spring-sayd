package ru.otus.hw.batch.config;

import java.util.ArrayList;
import java.util.List;
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
import ru.otus.hw.batch.writer.CustomMongoItemWriter;
import ru.otus.hw.batch.cache.AuthorCustomCache;
import ru.otus.hw.batch.cache.BookCustomCache;
import ru.otus.hw.batch.cache.GenreCustomCache;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.GenreMongo;
import ru.otus.hw.models.jpa.Book;
import ru.otus.hw.models.jpa.Genre;
import ru.otus.hw.repositories.jpa.BookJpaRepository;

@Configuration
@RequiredArgsConstructor
public class BookStepConfig {

  private final BookJpaRepository bookRepository;

  private final MongoTemplate mongoTemplate;

  private final PlatformTransactionManager platformTransactionManager;

  private final JobRepository jobRepository;

  private final AuthorCustomCache authorCache;

  private final BookCustomCache bookCache;

  private final GenreCustomCache genreCache;

  @Bean
  public Step bookMigration(
    RepositoryItemReader<Book> bookReader,
    ItemProcessor<Book, Pair<Book, BookMongo>> bookProcessor,
    CustomMongoItemWriter<Book, BookMongo> bookWriter
  ) {
    return new StepBuilder("bookMigrationStep", jobRepository)
      .<Book, Pair<Book, BookMongo>>chunk(5, platformTransactionManager)
      .reader(bookReader)
      .processor(bookProcessor)
      .writer(bookWriter)
      .allowStartIfComplete(true)
      .build();
  }

  @Bean
  public RepositoryItemReader<Book> bookReader() {
    return new RepositoryItemReaderBuilder<Book>()
      .name("bookReader")
      .repository(bookRepository)
      .methodName("findAll")
      .pageSize(20)
      .sorts(Map.of("id", Direction.ASC))
      .build();
  }

  @Bean
  public ItemProcessor<Book, Pair<Book, BookMongo>> bookProcessor() {
    return item -> {
      Long authorId = item.getAuthor() != null ? item.getAuthor().getId() : null;
      List<Long> genreIds = item.getGenres() != null ?
        item.getGenres().stream().map(Genre::getId).toList() : new ArrayList<>();
      AuthorMongo authorMongo = authorId != null ? authorCache.get(authorId) : null;
      List<GenreMongo> genresMongo =
        !genreIds.isEmpty() ? genreCache.get(genreIds) : new ArrayList<>();
      return Pair.of(item, new BookMongo(null, item.getTitle(), authorMongo, genresMongo));
    };
  }

  @Bean
  public CustomMongoItemWriter<Book, BookMongo> bookWriter() {
    return new CustomMongoItemWriter<>(mongoTemplate, bookCache);
  }
}
