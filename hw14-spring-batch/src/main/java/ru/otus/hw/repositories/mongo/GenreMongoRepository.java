package ru.otus.hw.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.otus.hw.models.mongo.GenreMongo;

public interface GenreMongoRepository extends MongoRepository<GenreMongo, String> {
}
