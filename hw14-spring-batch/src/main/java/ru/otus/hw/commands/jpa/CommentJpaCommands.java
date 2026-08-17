package ru.otus.hw.commands.jpa;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.jpa.CommentJpaConverter;
import ru.otus.hw.models.jpa.Comment;
import ru.otus.hw.services.jpa.CommentJpaService;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class CommentJpaCommands {

  private final CommentJpaService commentService;

  private final CommentJpaConverter commentConverter;

  @ShellMethod(value = "Find comment by id", key = "cbid")
  public String findCommentById(long id) {
    return commentService.findById(id)
      .map(commentConverter::commentToString)
      .orElse("Comment with id %d not found".formatted(id));
  }

  @ShellMethod(value = "Find comments by book id", key = "cbookid")
  public String findCommentsByBookId(long bookId) {
    return commentService.findByBookId(bookId).stream()
      .map(commentConverter::commentToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Insert comment", key = "cins")
  public String insertComment(String text, long bookId) {
    Comment savedComment = commentService.insert(text, bookId);
    return commentConverter.commentToString(savedComment);
  }

  @ShellMethod(value = "Update comment", key = "cupd")
  public String updateComment(long id, String text, long bookId) {
    Comment savedComment = commentService.update(id, text, bookId);
    return commentConverter.commentToString(savedComment);
  }

  @ShellMethod(value = "Delete comment by id", key = "cdel")
  public void deleteComment(long id) {
    commentService.deleteById(id);
  }
}
