package ru.otus.hw.services.mongo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ru.otus.hw.models.mongo.BookMongo;

public interface BookMongoService {

  Optional<BookMongo> findById(String id);

  List<BookMongo> findAll();

  BookMongo insert(String title, String authorId, Set<String> genresIds);

  BookMongo update(String id, String title, String authorId, Set<String> genresIds);

  void deleteById(String id);
}
