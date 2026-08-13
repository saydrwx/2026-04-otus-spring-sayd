package ru.otus.hw.service;

import java.util.List;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;

public interface BookService {

  BookDto findById(long id);

  List<BookDto> findAll();

  BookDto create(BookForm form);

  BookDto update(long id, BookForm form);

  void deleteById(long id);
}
