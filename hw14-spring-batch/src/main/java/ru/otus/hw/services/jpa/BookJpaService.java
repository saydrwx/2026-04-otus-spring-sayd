package ru.otus.hw.services.jpa;

import ru.otus.hw.models.jpa.Book;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BookJpaService {

  Optional<Book> findById(long id);

  List<Book> findAll();

  Book insert(String title, long authorId, Set<Long> genresIds);

  Book update(long id, String title, long authorId, Set<Long> genresIds);

  void deleteById(long id);
}
