package ru.otus.hw.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.exception.GenreNotFoundException;
import ru.otus.hw.mapper.GenreMapper;
import ru.otus.hw.model.Genre;
import ru.otus.hw.repository.GenreRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {

  private final GenreRepository genreRepository;

  private final GenreMapper genreMapper;

  @Override
  @CircuitBreaker(name = "genreServiceBreaker")
  public GenreDto findById(long id) {
    return genreMapper.toDto(findGenre(id));
  }

  @Override
  @CircuitBreaker(name = "genreServiceBreaker")
  public List<GenreDto> findAll() {
    return genreRepository.findAllByOrderByNameAsc().stream()
        .map(genreMapper::toDto)
        .toList();
  }

  @Override
  @Transactional
  @CircuitBreaker(name = "genreServiceBreaker")
  public GenreDto create(GenreForm form) {
    Genre genre = genreMapper.toEntity(form);
    return genreMapper.toDto(genreRepository.save(genre));
  }

  @Override
  @Transactional
  @CircuitBreaker(name = "genreServiceBreaker")
  public GenreDto update(long id, GenreForm form) {
    Genre genre = findGenre(id);
    genreMapper.updateEntity(form, genre);
    return genreMapper.toDto(genre);
  }

  @Override
  @Transactional
  @CircuitBreaker(name = "genreServiceBreaker")
  public void deleteById(long id) {
    Genre genre = findGenre(id);
    genreRepository.delete(genre);
  }

  private Genre findGenre(long id) {
    return genreRepository.findById(id)
        .orElseThrow(() -> new GenreNotFoundException(id));
  }
}
