package ru.otus.hw.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Document("comments")
public class Comment {

  @ToString.Include
  @EqualsAndHashCode.Include
  @Id
  private String id;

  @ToString.Include
  @EqualsAndHashCode.Include
  private String text;

  @DocumentReference(lazy = true)
  private Book book;
}
