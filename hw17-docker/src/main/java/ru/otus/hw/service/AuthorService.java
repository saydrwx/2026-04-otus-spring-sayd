package ru.otus.hw.service;

import java.util.List;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.AuthorForm;

public interface AuthorService {

  AuthorDto findById(long id);

  List<AuthorDto> findAll();

  AuthorDto create(AuthorForm form);

  AuthorDto update(long id, AuthorForm form);

  void deleteById(long id);
}
