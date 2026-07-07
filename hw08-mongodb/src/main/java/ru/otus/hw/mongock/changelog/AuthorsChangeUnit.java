package ru.otus.hw.mongock.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "authors-migration", order = "001", author = "saydrwx", runAlways = true)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class AuthorsChangeUnit {

  private final MongoTemplate mongoTemplate;

  @Execution
  public void execute() {
    mongoTemplate.dropCollection("authors");

    List<Document> authors = List.of(
      new Document().append("_id", "1").append("fullName", "Author_1"),
      new Document().append("_id", "2").append("fullName", "Author_2"),
      new Document().append("_id", "3").append("fullName", "Author_3")
    );

    mongoTemplate.insert(authors, "authors");
  }

  @RollbackExecution
  public void rollback() {
    mongoTemplate.dropCollection("authors");
  }
}
