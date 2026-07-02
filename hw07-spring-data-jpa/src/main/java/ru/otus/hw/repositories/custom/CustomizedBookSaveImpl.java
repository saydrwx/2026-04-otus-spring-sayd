package ru.otus.hw.repositories.custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Book;

class CustomizedBookSaveImpl implements CustomizedBookSave<Book> {

  @PersistenceContext
  private EntityManager em;

  @Override
  public <S extends Book> S save(S book) {
    if (book.getId() == 0) {
      em.persist(book);
      return book;
    }

    if (em.find(Book.class, book.getId()) == null) {
      throw new EntityNotFoundException("Book with id %d not found".formatted(book.getId()));
    }
    return em.merge(book);
  }
}
