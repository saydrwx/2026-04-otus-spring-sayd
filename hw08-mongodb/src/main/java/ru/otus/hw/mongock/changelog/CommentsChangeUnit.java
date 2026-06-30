package ru.otus.hw.mongock.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "comments-migration", order = "004", author = "saydrwx", runAlways = true)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class CommentsChangeUnit {

  private final MongoTemplate mongoTemplate;

  @Execution
  public void execute() {
    mongoTemplate.dropCollection("comments");

    List<Document> comments = List.of(
      new Document().append("_id", "1")
        .append("text", "BookTitle_1 comment")
        .append("book", "1"),
      new Document().append("_id", "2")
        .append("text", "BookTitle_2 comment")
        .append("book", "2"),
      new Document().append("_id", "3")
        .append("text", "BookTitle_3 comment")
        .append("book", "3")
    );

    mongoTemplate.insert(comments, "comments");
  }

  @RollbackExecution
  public void rollback() {
    mongoTemplate.dropCollection("comments");
  }
}
