package ru.otus.hw.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;
import ru.otus.hw.models.Author;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcAuthorRepository implements AuthorRepository {

  private final NamedParameterJdbcOperations jdbcOperations;

  @Override
  public List<Author> findAll() {
    return jdbcOperations.query(
      "SELECT id, full_name FROM authors",
      new AuthorRowMapper()
    );
  }

  @Override
  public Optional<Author> findById(long id) {
    var params = new MapSqlParameterSource()
      .addValue("id", id);
    List<Author> authors = jdbcOperations.query(
      "SELECT id, full_name FROM authors WHERE id = :id",
      params,
      new AuthorRowMapper()
    );
    Author author = DataAccessUtils.singleResult(authors);
    return Optional.ofNullable(author);
  }

  private static class AuthorRowMapper implements RowMapper<Author> {

    @Override
    public Author mapRow(ResultSet rs, int i) throws SQLException {
      long id = rs.getLong("id");
      String fullName = rs.getString("full_name");
      return new Author(id, fullName);
    }
  }
}
