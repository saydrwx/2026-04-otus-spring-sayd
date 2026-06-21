package ru.otus.hw.repositories;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.stereotype.Repository;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Book;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaBookRepository implements BookRepository {

  @PersistenceContext
  private EntityManager em;

  @Override
  public Optional<Book> findById(long id) {
    EntityGraph<?> entityGraph = em.getEntityGraph("books-entity-graph");
    return Optional.ofNullable(em.find(
      Book.class,
      id,
      Map.of(EntityGraphType.FETCH.getKey(), entityGraph)
    ));
  }

  @Override
  public List<Book> findAll() {
    EntityGraph<?> entityGraph = em.getEntityGraph("books-entity-graph");
    TypedQuery<Book> query = em.createQuery("SELECT b FROM Book b", Book.class);
    query.setHint(EntityGraphType.FETCH.getKey(), entityGraph);
    return query.getResultList();
  }

  @Override
  public Book save(Book book) {
    if (book.getId() == 0) {
      em.persist(book);
      return book;
    }

    if (em.find(Book.class, book.getId()) == null) {
      throw new EntityNotFoundException("Book with id %d not found".formatted(book.getId()));
    }
    return em.merge(book);
  }

  @Override
  public void deleteById(long id) {
    Book book = em.find(Book.class, id);
    if (book != null) {
      em.remove(book);
    }
  }
}
