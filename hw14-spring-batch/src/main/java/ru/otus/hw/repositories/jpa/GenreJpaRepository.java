package ru.otus.hw.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.models.jpa.Genre;

public interface GenreJpaRepository extends JpaRepository<Genre, Long> {
}
