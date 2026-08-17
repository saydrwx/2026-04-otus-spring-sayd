package ru.otus.hw.services.jpa;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.models.jpa.Genre;
import ru.otus.hw.repositories.jpa.GenreJpaRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GenreJpaServiceImpl implements GenreJpaService {

  private final GenreJpaRepository genreRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Genre> findAll() {
    return genreRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Genre> findAllByIds(final Set<Long> ids) {
    return genreRepository.findAllById(ids);
  }
}
