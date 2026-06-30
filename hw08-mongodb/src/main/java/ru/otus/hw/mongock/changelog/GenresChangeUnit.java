package ru.otus.hw.mongock.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "genres-migration", order = "002", author = "saydrwx", runAlways = true)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class GenresChangeUnit {

  private final MongoTemplate mongoTemplate;

  @Execution
  public void execute() {
    mongoTemplate.dropCollection("genres");

    List<Document> genres = List.of(
      new Document().append("_id", "1").append("name", "Genre_1"),
      new Document().append("_id", "2").append("name", "Genre_2"),
      new Document().append("_id", "3").append("name", "Genre_3"),
      new Document().append("_id", "4").append("name", "Genre_4"),
      new Document().append("_id", "5").append("name", "Genre_5"),
      new Document().append("_id", "6").append("name", "Genre_6")
    );

    mongoTemplate.insert(genres, "genres");
  }

  @RollbackExecution
  public void rollback() {
    mongoTemplate.dropCollection("genres");
  }
}
