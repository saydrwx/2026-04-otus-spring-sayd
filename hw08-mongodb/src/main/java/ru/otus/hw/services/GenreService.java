package ru.otus.hw.services;

import java.util.Set;
import ru.otus.hw.models.Genre;

import java.util.List;

public interface GenreService {

  List<Genre> findAll();

  List<Genre> findAllByIds(Set<String> ids);
}
