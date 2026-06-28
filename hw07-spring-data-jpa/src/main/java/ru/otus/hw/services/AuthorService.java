package ru.otus.hw.services;

import java.util.Optional;
import ru.otus.hw.models.Author;

import java.util.List;

public interface AuthorService {

  List<Author> findAll();

  Optional<Author> findById(long id);
}
