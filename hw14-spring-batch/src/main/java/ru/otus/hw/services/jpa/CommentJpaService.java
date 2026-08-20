package ru.otus.hw.services.jpa;

import java.util.List;
import java.util.Optional;
import ru.otus.hw.models.jpa.Comment;

public interface CommentJpaService {

  Optional<Comment> findById(long id);

  List<Comment> findByBookId(long bookId);

  Comment insert(String text, long bookId);

  Comment update(long id, String text, long bookId);

  void deleteById(long id);
}
