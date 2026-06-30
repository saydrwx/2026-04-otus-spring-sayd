package ru.otus.hw;

import java.util.List;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Comment;
import ru.otus.hw.models.Genre;
import ru.otus.hw.mongock.changelog.AuthorsChangeUnit;
import ru.otus.hw.mongock.changelog.BooksChangeUnit;
import ru.otus.hw.mongock.changelog.CommentsChangeUnit;
import ru.otus.hw.mongock.changelog.GenresChangeUnit;

@Component
@RequiredArgsConstructor
@Import({AuthorsChangeUnit.class, BooksChangeUnit.class, CommentsChangeUnit.class, GenresChangeUnit.class})
public class TestDataPreparer {

  @Autowired
  private final AuthorsChangeUnit authorsChangeUnit;

  @Autowired
  private final GenresChangeUnit genresChangeUnit;

  @Autowired
  private final BooksChangeUnit booksChangeUnit;

  @Autowired
  private final CommentsChangeUnit commentsChangeUnit;

  @Getter
  private List<Author> dbAuthors;

  @Getter
  private List<Genre> dbGenres;

  @Getter
  private List<Book> dbBooks;

  @Getter
  private List<Comment> dbComments;

  public void prepare() {
    authorsChangeUnit.execute();
    genresChangeUnit.execute();
    booksChangeUnit.execute();
    commentsChangeUnit.execute();

    dbAuthors = prepareDbAuthors();
    dbGenres = prepareDbGenres();
    dbBooks = prepareDbBooks(dbAuthors, dbGenres);
    dbComments = prepareDbComments(dbBooks);
  }

  private static List<Author> prepareDbAuthors() {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Author(String.valueOf(id), "Author_" + id))
      .toList();
  }

  private static List<Genre> prepareDbGenres() {
    return IntStream.range(1, 7).boxed()
      .map(id -> new Genre(String.valueOf(id), "Genre_" + id))
      .toList();
  }

  private static List<Book> prepareDbBooks(List<Author> dbAuthors, List<Genre> dbGenres) {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Book(String.valueOf(id),
        "BookTitle_" + id,
        dbAuthors.get(id - 1),
        dbGenres.subList((id - 1) * 2, (id - 1) * 2 + 2)
      ))
      .toList();
  }

  public static List<Comment> prepareDbComments(List<Book> dbBooks) {
    return IntStream.range(1, 4).boxed()
      .map(id -> new Comment(String.valueOf(id),
        "BookTitle_" + id + " comment",
        dbBooks.get(id - 1)
      ))
      .toList();
  }
}
