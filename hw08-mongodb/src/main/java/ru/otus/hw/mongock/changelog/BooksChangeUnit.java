package ru.otus.hw.mongock.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "books-migration", order = "003", author = "saydrwx", runAlways = true)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class BooksChangeUnit {

  private final MongoTemplate mongoTemplate;

  @Execution
  public void execute() {
    mongoTemplate.dropCollection("books");

    List<Document> books = List.of(
      new Document().append("_id", "1")
        .append("title", "BookTitle_1")
        .append("author", "1")
        .append("genres", List.of("1", "2")),
      new Document().append("_id", "2")
        .append("title", "BookTitle_2")
        .append("author", "2")
        .append("genres", List.of("3", "4")),
      new Document().append("_id", "3")
        .append("title", "BookTitle_3")
        .append("author", "3")
        .append("genres", List.of("5", "6"))
    );

    mongoTemplate.insert(books, "books");
  }

  @RollbackExecution
  public void rollback() {
    mongoTemplate.dropCollection("books");
  }
}
