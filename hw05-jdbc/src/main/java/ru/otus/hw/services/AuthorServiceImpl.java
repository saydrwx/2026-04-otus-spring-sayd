package ru.otus.hw.services;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.models.Author;
import ru.otus.hw.repositories.AuthorRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AuthorServiceImpl implements AuthorService {

  private final AuthorRepository authorRepository;

  @Override
  public List<Author> findAll() {
    return authorRepository.findAll();
  }

  @Override
  public Optional<Author> findById(long id) {
    return authorRepository.findById(id);
  }
}
