package ru.otus.hw.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.repositories.mongo.custom.CustomizedBookSaveMongo;

public interface BookMongoRepository extends MongoRepository<BookMongo, String>,
  CustomizedBookSaveMongo<BookMongo> {

}
