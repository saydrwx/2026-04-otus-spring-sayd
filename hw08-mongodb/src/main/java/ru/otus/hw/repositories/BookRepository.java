package ru.otus.hw.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.otus.hw.models.Book;
import ru.otus.hw.repositories.custom.CustomizedBookSave;

public interface BookRepository extends MongoRepository<Book, String>, CustomizedBookSave<Book> {

}
