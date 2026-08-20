package ru.otus.hw.commands.mongo;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.mongo.CommentMongoConverter;
import ru.otus.hw.models.mongo.CommentMongo;
import ru.otus.hw.services.mongo.CommentMongoService;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class CommentMongoCommands {

  private final CommentMongoService commentService;

  private final CommentMongoConverter commentConverter;

  @ShellMethod(value = "Find comment by id", key = "cbidm")
  public String findCommentById(String id) {
    return commentService.findById(id)
      .map(commentConverter::commentToString)
      .orElse("Comment with id %s not found".formatted(id));
  }

  @ShellMethod(value = "Find comments by book id", key = "cbookidm")
  public String findCommentsByBookId(String bookId) {
    return commentService.findByBookId(bookId).stream()
      .map(commentConverter::commentToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Insert comment", key = "cinsm")
  public String insertComment(String text, String bookId) {
    CommentMongo savedComment = commentService.insert(text, bookId);
    return commentConverter.commentToString(savedComment);
  }

  @ShellMethod(value = "Update comment", key = "cupdm")
  public String updateComment(String id, String text, String bookId) {
    CommentMongo savedComment = commentService.update(id, text, bookId);
    return commentConverter.commentToString(savedComment);
  }

  @ShellMethod(value = "Delete comment by id", key = "cdelm")
  public void deleteComment(String id) {
    commentService.deleteById(id);
  }
}
