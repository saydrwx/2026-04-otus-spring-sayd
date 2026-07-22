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
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.service.AuthorService;

@RestController
@RequiredArgsConstructor
public class AuthorController {

  private final AuthorService authorService;

  @GetMapping("/api/authors")
  public List<AuthorDto> findAll() {
    return authorService.findAll();
  }

  @GetMapping("/api/authors/{id}")
  public AuthorDto findById(@PathVariable long id) {
    return authorService.findById(id);
  }

  @PostMapping("/api/authors")
  public ResponseEntity<AuthorDto> create(@Valid @RequestBody AuthorForm form) {
    AuthorDto author = authorService.create(form);
    return ResponseEntity.created(URI.create("/api/authors/" + author.id())).body(author);
  }

  @PutMapping("/api/authors/{id}")
  public AuthorDto update(@PathVariable long id, @Valid @RequestBody AuthorForm form) {
    return authorService.update(id, form);
  }

  @DeleteMapping("/api/authors/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    authorService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
