package ru.otus.hw.services.jpa;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.models.jpa.Author;
import ru.otus.hw.repositories.jpa.AuthorJpaRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AuthorJpaServiceImpl implements AuthorJpaService {

  private final AuthorJpaRepository authorRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Author> findAll() {
    return authorRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Author> findById(long id) {
    return authorRepository.findById(id);
  }
}
