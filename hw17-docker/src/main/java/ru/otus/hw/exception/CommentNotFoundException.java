package ru.otus.hw.exception;

public final class CommentNotFoundException extends AbstractEntityNotFoundException {

  public CommentNotFoundException(long commentId, long bookId) {
    super(
        "Comment with id %d was not found for book %d".formatted(commentId, bookId),
        "exception.comment.not-found",
        commentId,
        bookId
    );
  }

  public CommentNotFoundException(long commentId) {
    super(
      "Comment with id %d was not found".formatted(commentId),
      "exception.comment.not-found",
      commentId
    );
  }
}
