package ru.otus.hw.models.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document("books")
public class BookMongo {

  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;

  @ToString.Include
  @EqualsAndHashCode.Include
  private String title;

  @DocumentReference(lazy = true)
  private AuthorMongo author;

  @DocumentReference(lazy = true)
  private List<GenreMongo> genres;
}
