package ru.otus.hw.repositories.mongo.custom;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.mongo.BookMongo;

@RequiredArgsConstructor
class CustomizedBookSaveMongoImpl implements CustomizedBookSaveMongo<BookMongo> {

  private final MongoTemplate mongoTemplate;

  @Override
  public <S extends BookMongo> S save(S book) {
    if (book.getId() == null) {
      return mongoTemplate.insert(book);
    }

    Query query = Query.query(Criteria.where("_id").is(book.getId()));

    if (!mongoTemplate.exists(query, BookMongo.class)) {
      throw new EntityNotFoundException(
        "Book with id %s not found".formatted(book.getId())
      );
    }

    return mongoTemplate.save(book);
  }
}
