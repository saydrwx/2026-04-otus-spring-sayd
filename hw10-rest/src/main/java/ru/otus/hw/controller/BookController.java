package ru.otus.hw.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.service.BookService;

@RestController
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @GetMapping("/api/books")
  public List<BookDto> findAll() {
    return bookService.findAll();
  }

  @GetMapping("/api/books/{id}")
  public BookDto findById(@PathVariable long id) {
    return bookService.findById(id);
  }

  @PostMapping("/api/books")
  public ResponseEntity<BookDto> create(@Valid @RequestBody BookForm form) {
    BookDto book = bookService.create(form);
    return ResponseEntity.created(URI.create("/api/books/" + book.id())).body(book);
  }

  @PutMapping("/api/books/{id}")
  public BookDto update(@PathVariable long id, @Valid @RequestBody BookForm form) {
    return bookService.update(id, form);
  }

  @DeleteMapping("/api/books/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    bookService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
