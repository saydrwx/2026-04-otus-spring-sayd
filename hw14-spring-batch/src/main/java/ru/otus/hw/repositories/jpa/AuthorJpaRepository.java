package ru.otus.hw.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.models.jpa.Author;

public interface AuthorJpaRepository extends JpaRepository<Author, Long> {
}
