package ru.otus.hw.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.otus.hw.batch.cache.AuthorCustomCache;
import ru.otus.hw.batch.cache.BookCustomCache;
import ru.otus.hw.batch.cache.GenreCustomCache;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.CommentMongo;
import ru.otus.hw.models.mongo.GenreMongo;

@SpringBootTest(properties = {
  "spring.batch.job.enabled=false",
  "spring.shell.command.version.enabled=false",
  "spring.shell.interactive.enabled=false",
  "spring.main.allow-circular-references=true",
  "spring.data.mongodb.database=migration-job-test",
  "spring.data.mongodb.port=0",
  "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class MigrationJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("migrationJob")
  private Job migrationJob;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private AuthorCustomCache authorCache;

  @Autowired
  private GenreCustomCache genreCache;

  @Autowired
  private BookCustomCache bookCache;

  @Test
  void shouldMigrateCompleteJpaGraphToMongoInDependencyOrder() throws Exception {
    seedStaleState();
    var parameters = new JobParametersBuilder()
      .addLong("integrationTestRun", System.nanoTime())
      .toJobParameters();

    var execution = jobLauncher.run(migrationJob, parameters);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertStepMetrics(execution.getStepExecutions().stream()
      .collect(Collectors.toMap(StepExecution::getStepName, Function.identity())));
    assertAuthorsAndGenres();
    assertBooks();
    assertComments();
  }

  private void seedStaleState() {
    var staleAuthor = new AuthorMongo("stale-author", "Stale author");
    var staleGenre = new GenreMongo("stale-genre", "Stale genre");
    var staleBook = new BookMongo("stale-book", "Stale book", staleAuthor,
      java.util.List.of(staleGenre));
    mongoTemplate.insert(staleAuthor);
    mongoTemplate.insert(staleGenre);
    mongoTemplate.insert(staleBook);
    mongoTemplate.insert(new CommentMongo("stale-comment", "Stale comment", staleBook));
    authorCache.put(1L, staleAuthor);
    genreCache.put(1L, staleGenre);
    bookCache.put(1L, staleBook);
  }

  private static void assertStepMetrics(Map<String, StepExecution> steps) {
    assertThat(steps).containsOnlyKeys(
      "cleanUpStep",
      "authorMigrationStep",
      "genreMigrationStep",
      "bookMigrationStep",
      "commentMigrationStep"
    );
    assertCompletedWithCounts(steps.get("cleanUpStep"), 0, 0);
    assertCompletedWithCounts(steps.get("authorMigrationStep"), 3, 3);
    assertCompletedWithCounts(steps.get("genreMigrationStep"), 6, 6);
    assertCompletedWithCounts(steps.get("bookMigrationStep"), 3, 3);
    assertCompletedWithCounts(steps.get("commentMigrationStep"), 3, 3);

    assertThat(steps.get("bookMigrationStep").getStartTime())
      .isAfterOrEqualTo(steps.get("authorMigrationStep").getEndTime())
      .isAfterOrEqualTo(steps.get("genreMigrationStep").getEndTime());
    assertThat(steps.get("commentMigrationStep").getStartTime())
      .isAfterOrEqualTo(steps.get("bookMigrationStep").getEndTime());
  }

  private static void assertCompletedWithCounts(
    StepExecution step, long expectedReadCount, long expectedWriteCount
  ) {
    assertThat(step.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(step.getReadCount()).isEqualTo(expectedReadCount);
    assertThat(step.getWriteCount()).isEqualTo(expectedWriteCount);
  }

  private void assertAuthorsAndGenres() {
    assertThat(mongoTemplate.findAll(AuthorMongo.class))
      .hasSize(3)
      .allSatisfy(author -> assertThat(author.getId()).isNotBlank())
      .extracting(AuthorMongo::getFullName)
      .containsExactlyInAnyOrder("Author_1", "Author_2", "Author_3");

    assertThat(mongoTemplate.findAll(GenreMongo.class))
      .hasSize(6)
      .allSatisfy(genre -> assertThat(genre.getId()).isNotBlank())
      .extracting(GenreMongo::getName)
      .containsExactlyInAnyOrder(
        "Genre_1", "Genre_2", "Genre_3", "Genre_4", "Genre_5", "Genre_6");
  }

  private void assertBooks() {
    var books = mongoTemplate.findAll(BookMongo.class);

    assertThat(books).hasSize(3)
      .allSatisfy(book -> {
        assertThat(book.getId()).isNotBlank();
        assertThat(book.getAuthor()).isNotNull();
        assertThat(book.getAuthor().getId()).isNotBlank();
        assertThat(book.getGenres()).hasSize(2)
          .allSatisfy(genre -> assertThat(genre.getId()).isNotBlank());
      });
    assertThat(books).extracting(BookMongo::getTitle)
      .containsExactlyInAnyOrder("BookTitle_1", "BookTitle_2", "BookTitle_3");

    books.forEach(book -> {
      var number = book.getTitle().substring("BookTitle_".length());
      int bookNumber = Integer.parseInt(number);
      assertThat(book.getAuthor().getFullName()).isEqualTo("Author_" + number);
      assertThat(book.getGenres()).extracting(GenreMongo::getName)
        .containsExactlyInAnyOrder(
          "Genre_" + (bookNumber * 2 - 1),
          "Genre_" + (bookNumber * 2)
        );
    });
  }

  private void assertComments() {
    var comments = mongoTemplate.findAll(CommentMongo.class);

    assertThat(comments).hasSize(3)
      .allSatisfy(comment -> {
        assertThat(comment.getId()).isNotBlank();
        assertThat(comment.getBook()).isNotNull();
        assertThat(comment.getBook().getId()).isNotBlank();
        assertThat(comment.getText())
          .isEqualTo(comment.getBook().getTitle() + " comment");
      });
  }
}
