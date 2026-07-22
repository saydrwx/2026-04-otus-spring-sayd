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
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.service.GenreService;

@RestController
@RequiredArgsConstructor
public class GenreController {

  private final GenreService genreService;

  @GetMapping("/api/genres")
  public List<GenreDto> findAll() {
    return genreService.findAll();
  }

  @GetMapping("/api/genres/{id}")
  public GenreDto findById(@PathVariable long id) {
    return genreService.findById(id);
  }

  @PostMapping("/api/genres")
  public ResponseEntity<GenreDto> create(@Valid @RequestBody GenreForm form) {
    GenreDto genre = genreService.create(form);
    return ResponseEntity.created(URI.create("/api/genres/" + genre.id())).body(genre);
  }

  @PutMapping("/api/genres/{id}")
  public GenreDto update(@PathVariable long id, @Valid @RequestBody GenreForm form) {
    return genreService.update(id, form);
  }

  @DeleteMapping("/api/genres/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    genreService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
