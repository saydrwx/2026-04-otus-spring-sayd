package ru.otus.hw.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findAllByBookIdOrderByIdAsc(long bookId);
}
