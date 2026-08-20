package ru.otus.hw.services.jpa;

import java.util.Optional;
import ru.otus.hw.models.jpa.Author;

import java.util.List;

public interface AuthorJpaService {

  List<Author> findAll();

  Optional<Author> findById(long id);
}
