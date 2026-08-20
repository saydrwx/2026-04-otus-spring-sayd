package ru.otus.hw.services.mongo;

import java.util.List;
import java.util.Optional;
import ru.otus.hw.models.mongo.CommentMongo;

public interface CommentMongoService {

  Optional<CommentMongo> findById(String id);

  List<CommentMongo> findByBookId(String bookId);

  CommentMongo insert(String text, String bookId);

  CommentMongo update(String id, String text, String bookId);

  void deleteById(String id);
}
