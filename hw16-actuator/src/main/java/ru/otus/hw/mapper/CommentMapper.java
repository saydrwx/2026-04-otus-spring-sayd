package ru.otus.hw.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.model.Book;
import ru.otus.hw.model.Comment;

@Component
public class CommentMapper {

  public CommentDto toDto(Comment comment) {
    return new CommentDto(comment.getId(), comment.getText(), comment.getBook().getId());
  }

  public CommentForm toForm(CommentDto comment) {
    return new CommentForm(comment.text());
  }

  public Comment toEntity(CommentForm form, Book book) {
    return new Comment(normalize(form.getText()), book);
  }

  public void updateEntity(CommentForm form, Comment comment) {
    comment.setText(normalize(form.getText()));
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
