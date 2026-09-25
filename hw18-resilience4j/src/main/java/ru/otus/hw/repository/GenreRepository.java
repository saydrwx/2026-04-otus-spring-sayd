package ru.otus.hw.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.model.Genre;

public interface GenreRepository extends JpaRepository<Genre, Long> {

  List<Genre> findAllByOrderByNameAsc();
}
