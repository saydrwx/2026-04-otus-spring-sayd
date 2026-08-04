package ru.otus.hw.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "books")
public class Book {

  @EqualsAndHashCode.Include
  @ToString.Include
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @EqualsAndHashCode.Include
  @ToString.Include
  private String title;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private Author author;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "books_genres",
    joinColumns = @JoinColumn(name = "book_id"),
    inverseJoinColumns = @JoinColumn(name = "genre_id")
  )
  private Set<Genre> genres = new LinkedHashSet<>();

  public Book(String title, Author author, Collection<Genre> genres) {
    this.title = title;
    this.author = author;
    this.genres.addAll(genres);
  }

  public void replaceGenres(Collection<Genre> genres) {
    this.genres.clear();
    this.genres.addAll(genres);
  }
}
