package ru.otus.hw.services.jpa;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.jpa.Book;
import ru.otus.hw.models.jpa.Comment;
import ru.otus.hw.repositories.jpa.BookJpaRepository;
import ru.otus.hw.repositories.jpa.CommentJpaRepository;

@RequiredArgsConstructor
@Service
public class CommentJpaServiceImpl implements CommentJpaService {

  private final CommentJpaRepository commentRepository;

  private final BookJpaRepository bookRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<Comment> findById(long id) {
    return commentRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Comment> findByBookId(long bookId) {
    return commentRepository.findByBookId(bookId);
  }

  @Override
  @Transactional
  public Comment insert(String text, long bookId) {
    return save(0L, text, bookId);
  }

  @Override
  @Transactional
  public Comment update(long id, String text, long bookId) {
    return save(id, text, bookId);
  }

  @Override
  @Transactional
  public void deleteById(long id) {
    commentRepository.deleteById(id);
  }

  private Comment save(long id, String text, long bookId) {
    Book book = bookRepository.findById(bookId)
      .orElseThrow(
        () -> new EntityNotFoundException("Book with id %d not found".formatted(bookId)));
    var comment = new Comment(id, text, book);
    return commentRepository.save(comment);
  }
}
