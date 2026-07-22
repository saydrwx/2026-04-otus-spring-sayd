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
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.service.CommentService;

@RestController
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @GetMapping("/api/books/{bookId}/comments")
  public List<CommentDto> findAllByBookId(@PathVariable long bookId) {
    return commentService.findAllByBookId(bookId);
  }

  @GetMapping("/api/comments/{commentId}")
  public CommentDto findById(@PathVariable long commentId) {
    return commentService.findById(commentId);
  }

  @PostMapping("/api/books/{bookId}/comments")
  public ResponseEntity<CommentDto> create(
    @PathVariable long bookId,
    @Valid @RequestBody CommentForm form
  ) {
    CommentDto comment = commentService.create(bookId, form);
    return ResponseEntity.created(URI.create("/api/comments/" + comment.id())).body(comment);
  }

  @PutMapping("/api/comments/{commentId}")
  public CommentDto update(
    @PathVariable long commentId,
    @Valid @RequestBody CommentForm form
  ) {
    return commentService.update(commentId, form);
  }

  @DeleteMapping("/api/comments/{commentId}")
  public ResponseEntity<Void> delete(@PathVariable long commentId) {
    commentService.deleteById(commentId);
    return ResponseEntity.noContent().build();
  }
}
