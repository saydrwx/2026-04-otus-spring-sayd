package ru.otus.hw.services.mongo;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.models.mongo.GenreMongo;
import ru.otus.hw.repositories.mongo.GenreMongoRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GenreServiceMongoImpl implements GenreMongoService {

  private final GenreMongoRepository genreRepository;

  @Override
  public List<GenreMongo> findAll() {
    return genreRepository.findAll();
  }

  @Override
  public List<GenreMongo> findAllByIds(final Set<String> ids) {
    return genreRepository.findAllById(ids);
  }
}
