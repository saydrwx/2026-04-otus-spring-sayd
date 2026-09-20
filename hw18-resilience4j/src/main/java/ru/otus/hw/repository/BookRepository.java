package ru.otus.hw.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.model.Book;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

  @Override
  @EntityGraph(attributePaths = {"author", "genres"})
  Optional<Book> findById(Long id);

  @EntityGraph(attributePaths = {"author", "genres"})
  List<Book> findAllByOrderByTitleAsc();
}
