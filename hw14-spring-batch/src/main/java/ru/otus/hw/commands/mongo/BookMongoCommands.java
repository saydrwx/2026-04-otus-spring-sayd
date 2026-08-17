package ru.otus.hw.commands.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.mongo.BookMongoConverter;
import ru.otus.hw.services.mongo.BookMongoService;

import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class BookMongoCommands {

  private final BookMongoService bookService;

  private final BookMongoConverter bookConverter;

  @ShellMethod(value = "Find all books", key = "abm")
  public String findAllBooks() {
    return bookService.findAll().stream()
      .map(bookConverter::bookToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Find book by id", key = "bbidm")
  public String findBookById(String id) {
    return bookService.findById(id)
      .map(bookConverter::bookToString)
      .orElse("Book with id %s not found".formatted(id));
  }

  // bins newBook 1 1,6
  @ShellMethod(value = "Insert book", key = "binsm")
  public String insertBook(String title, String authorId, Set<String> genresIds) {
    var savedBook = bookService.insert(title, authorId, genresIds);
    return bookConverter.bookToString(savedBook);
  }

  // bupd 4 editedBook 3 2,5
  @ShellMethod(value = "Update book", key = "bupdm")
  public String updateBook(String id, String title, String authorId, Set<String> genresIds) {
    var savedBook = bookService.update(id, title, authorId, genresIds);
    return bookConverter.bookToString(savedBook);
  }

  // bdel 4
  @ShellMethod(value = "Delete book by id", key = "bdelm")
  public void deleteBook(String id) {
    bookService.deleteById(id);
  }
}
