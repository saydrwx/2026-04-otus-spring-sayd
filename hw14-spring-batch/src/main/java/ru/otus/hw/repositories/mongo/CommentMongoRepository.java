package ru.otus.hw.repositories.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.otus.hw.models.mongo.CommentMongo;

public interface CommentMongoRepository extends MongoRepository<CommentMongo, String> {

  List<CommentMongo> findByBookId(String bookId);
}
