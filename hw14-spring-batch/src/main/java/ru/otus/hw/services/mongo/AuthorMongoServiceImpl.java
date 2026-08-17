package ru.otus.hw.services.mongo;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.models.mongo.AuthorMongo;
import ru.otus.hw.repositories.mongo.AuthorMongoRepository;

@RequiredArgsConstructor
@Service
public class AuthorMongoServiceImpl implements AuthorMongoService {

  private final AuthorMongoRepository authorRepository;

  @Override
  public List<AuthorMongo> findAll() {
    return authorRepository.findAll();
  }

  @Override
  public Optional<AuthorMongo> findById(String id) {
    return authorRepository.findById(id);
  }
}
