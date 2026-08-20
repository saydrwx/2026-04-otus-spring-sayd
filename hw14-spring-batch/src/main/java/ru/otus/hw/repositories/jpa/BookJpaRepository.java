package ru.otus.hw.repositories.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.models.jpa.Book;

import java.util.List;
import java.util.Optional;
import ru.otus.hw.repositories.jpa.custom.CustomizedBookSaveJpa;

public interface BookJpaRepository extends JpaRepository<Book, Long>, CustomizedBookSaveJpa<Book> {

  @Override
  @EntityGraph(value = "books-entity-graph", type = EntityGraphType.FETCH)
  Optional<Book> findById(Long id);

  @Override
  @EntityGraph(value = "books-entity-graph", type = EntityGraphType.FETCH)
  List<Book> findAll();
}
