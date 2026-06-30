package ru.otus.hw.commands;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.CommentConverter;
import ru.otus.hw.models.Comment;
import ru.otus.hw.services.CommentService;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class CommentCommands {

  private final CommentService commentService;

  private final CommentConverter commentConverter;

  @ShellMethod(value = "Find comment by id", key = "cbid")
  public String findCommentById(String id) {
    return commentService.findById(id)
      .map(commentConverter::commentToString)
      .orElse("Comment with id %s not found".formatted(id));
  }

  @ShellMethod(value = "Find comments by book id", key = "cbookid")
  public String findCommentsByBookId(String bookId) {
    return commentService.findByBookId(bookId).stream()
      .map(commentConverter::commentToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Insert comment", key = "cins")
  public String insertComment(String text, String bookId) {
    Comment savedComment = commentService.insert(text, bookId);
    return commentConverter.commentToString(savedComment);
  }

  @ShellMethod(value = "Update comment", key = "cupd")
  public String updateComment(String id, String text, String bookId) {
    Comment savedComment = commentService.update(id, text, bookId);
    return commentConverter.commentToString(savedComment);
  }

  @ShellMethod(value = "Delete comment by id", key = "cdel")
  public void deleteComment(String id) {
    commentService.deleteById(id);
  }
}
