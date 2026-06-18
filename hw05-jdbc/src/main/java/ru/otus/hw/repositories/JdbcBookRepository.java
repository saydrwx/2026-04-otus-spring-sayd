package ru.otus.hw.repositories;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcBookRepository implements BookRepository {

  private final GenreRepository genreRepository;

  private final NamedParameterJdbcOperations jdbcOperations;

  @Override
  public Optional<Book> findById(long id) {
    var params = new MapSqlParameterSource()
      .addValue("id", id);
    Book book = jdbcOperations.query(
      """
        SELECT
          b.id,
          b.title,
          b.author_id,
          a.full_name AS author_full_name,
          bg.genre_id,
          g.name AS genre_name
        FROM books b
          INNER JOIN authors a ON b.author_id = a.id
          INNER JOIN books_genres bg ON b.id = bg.book_id
          INNER JOIN genres g ON bg.genre_id = g.id
        WHERE b.id = :id
        """,
      params,
      new BookResultSetExtractor()
    );
    return Optional.ofNullable(book);
  }

  @Override
  public List<Book> findAll() {
    var genres = genreRepository.findAll();
    var books = getAllBooksWithoutGenres();
    var relations = getAllGenreRelations();
    mergeBooksInfo(books, genres, relations);
    return books;
  }

  @Override
  public Book save(Book book) {
    if (book.getId() == 0) {
      return insert(book);
    }
    return update(book);
  }

  @Override
  public void deleteById(long id) {
    var params = new MapSqlParameterSource()
      .addValue("id", id);
    jdbcOperations.update("DELETE FROM books WHERE id = :id", params);
  }

  private List<Book> getAllBooksWithoutGenres() {
    return jdbcOperations.query(
      """
        SELECT
          b.id,
          b.title,
          b.author_id,
          a.full_name AS author_full_name
        FROM books b
          INNER JOIN authors a ON b.author_id = a.id
        """,
      new BookRowMapper()
    );
  }

  private List<BookGenreRelation> getAllGenreRelations() {
    return jdbcOperations.query(
      "SELECT book_id, genre_id FROM books_genres",
      new BookGenreRelationRowMapper()
    );
  }

  private void mergeBooksInfo(List<Book> booksWithoutGenres, List<Genre> genres,
    List<BookGenreRelation> relations) {
    booksWithoutGenres.forEach(book -> {
      Set<Long> filteredGenreIds = relations.stream()
        .filter(relation -> relation.bookId == book.getId())
        .map(BookGenreRelation::genreId)
        .collect(Collectors.toSet());
      List<Genre> filteredGenres = genres.stream()
        .filter(genre -> filteredGenreIds.contains(genre.getId()))
        .toList();
      book.setGenres(filteredGenres);
    });
  }

  private Book insert(Book book) {
    var params = new MapSqlParameterSource()
      .addValue("title", book.getTitle())
      .addValue("author_id", book.getAuthor().getId());

    var keyHolder = new GeneratedKeyHolder();

    jdbcOperations.update(
      "INSERT INTO books (title, author_id) VALUES (:title, :author_id)",
      params,
      keyHolder,
      new String[]{"id"}
    );

    //noinspection DataFlowIssue
    book.setId(keyHolder.getKeyAs(Long.class));
    batchInsertGenresRelationsFor(book);
    return book;
  }

  private Book update(Book book) {
    var params = new MapSqlParameterSource()
      .addValue("book_id", book.getId())
      .addValue("title", book.getTitle())
      .addValue("author_id", book.getAuthor().getId());

    int numRowsUpdated = jdbcOperations.update(
      "UPDATE books SET title = :title, author_id = :author_id WHERE id = :book_id",
      params
    );

    if (numRowsUpdated == 0) {
      throw new EntityNotFoundException("Book with id %d not found".formatted(book.getId()));
    }

    removeGenresRelationsFor(book);
    batchInsertGenresRelationsFor(book);

    return book;
  }

  private void batchInsertGenresRelationsFor(Book book) {
    List<BookGenreRelation> relations = book.getGenres().stream()
      .map(genre -> new BookGenreRelation(book.getId(), genre.getId()))
      .toList();
    jdbcOperations.batchUpdate(
      "INSERT INTO books_genres (book_id, genre_id) VALUES (:bookId, :genreId)",
      SqlParameterSourceUtils.createBatch(relations)
    );
  }

  private void removeGenresRelationsFor(Book book) {
    var params = new MapSqlParameterSource()
      .addValue("book_id", book.getId());
    jdbcOperations.update("DELETE FROM books_genres WHERE book_id = :book_id", params);
  }

  private static class BookRowMapper implements RowMapper<Book> {

    @Override
    public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
      long authorId = rs.getLong("author_id");
      String authorFullName = rs.getString("author_full_name");
      var author = new Author(authorId, authorFullName);

      long bookId = rs.getLong("id");
      String title = rs.getString("title");

      return new Book(bookId, title, author, null);
    }
  }

  private static class BookGenreRelationRowMapper implements RowMapper<BookGenreRelation> {

    @Override
    public BookGenreRelation mapRow(ResultSet rs, int rowNum) throws SQLException {
      long bookId = rs.getLong("book_id");
      long genreId = rs.getLong("genre_id");
      return new BookGenreRelation(bookId, genreId);
    }
  }

  @RequiredArgsConstructor
  private static class BookResultSetExtractor implements ResultSetExtractor<Book> {

    @Override
    public Book extractData(ResultSet rs) throws SQLException, DataAccessException {

      if (rs.isBeforeFirst()) {
        var book = new Book();
        var genres = new ArrayList<Genre>();
        book.setGenres(genres);

        while (rs.next()) {
          long bookId = rs.getLong("id");
          String title = rs.getString("title");
          book.setId(bookId);
          book.setTitle(title);

          long authorId = rs.getLong("author_id");
          String authorFullName = rs.getString("author_full_name");
          var author = new Author(authorId, authorFullName);
          book.setAuthor(author);

          long genreId = rs.getLong("genre_id");
          String genreName = rs.getString("genre_name");
          var genre = new Genre(genreId, genreName);
          genres.add(genre);

        }

        return book;
      }

      return null;
    }
  }

  private record BookGenreRelation(long bookId, long genreId) {

  }
}
