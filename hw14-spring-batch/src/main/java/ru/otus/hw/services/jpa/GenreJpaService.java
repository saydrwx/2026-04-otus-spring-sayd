package ru.otus.hw.services.jpa;

import java.util.Set;
import ru.otus.hw.models.jpa.Genre;

import java.util.List;

public interface GenreJpaService {

  List<Genre> findAll();

  List<Genre> findAllByIds(Set<Long> ids);
}
