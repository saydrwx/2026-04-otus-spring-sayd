package ru.otus.hw.repositories.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.models.jpa.Comment;

public interface CommentJpaRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByBookId(long bookId);
}
