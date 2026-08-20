package ru.otus.hw.services.mongo;

import java.util.Optional;
import ru.otus.hw.models.mongo.AuthorMongo;

import java.util.List;

public interface AuthorMongoService {

  List<AuthorMongo> findAll();

  Optional<AuthorMongo> findById(String id);
}
