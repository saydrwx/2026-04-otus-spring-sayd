package ru.otus.hw.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

  List<Author> findAllByOrderByFullNameAsc();
}
