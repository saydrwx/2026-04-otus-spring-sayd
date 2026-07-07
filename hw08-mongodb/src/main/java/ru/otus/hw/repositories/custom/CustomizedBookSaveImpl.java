package ru.otus.hw.repositories.custom;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Book;

@RequiredArgsConstructor
class CustomizedBookSaveImpl implements CustomizedBookSave<Book> {

  private final MongoTemplate mongoTemplate;

  @Override
  public <S extends Book> S save(S book) {
    if (book.getId() == null) {
      return mongoTemplate.insert(book);
    }

    Query query = Query.query(Criteria.where("_id").is(book.getId()));

    if (!mongoTemplate.exists(query, Book.class)) {
      throw new EntityNotFoundException(
        "Book with id %s not found".formatted(book.getId())
      );
    }

    return mongoTemplate.save(book);
  }
}
