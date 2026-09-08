package ru.otus.hw.service;

import java.util.List;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;

public interface GenreService {

  GenreDto findById(long id);

  List<GenreDto> findAll();

  GenreDto create(GenreForm form);

  GenreDto update(long id, GenreForm form);

  void deleteById(long id);
}
