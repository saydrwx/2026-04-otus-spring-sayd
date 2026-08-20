package ru.otus.hw.services.mongo;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.mongo.BookMongo;
import ru.otus.hw.models.mongo.CommentMongo;
import ru.otus.hw.repositories.mongo.BookMongoRepository;
import ru.otus.hw.repositories.mongo.CommentMongoRepository;

@RequiredArgsConstructor
@Service
public class CommentMongoServiceImpl implements CommentMongoService {

  private final CommentMongoRepository commentRepository;

  private final BookMongoRepository bookRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<CommentMongo> findById(String id) {
    return commentRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CommentMongo> findByBookId(String bookId) {
    return commentRepository.findByBookId(bookId);
  }

  @Override
  @Transactional
  public CommentMongo insert(String text, String bookId) {
    return save(null, text, bookId);
  }

  @Override
  @Transactional
  public CommentMongo update(String id, String text, String bookId) {
    return save(id, text, bookId);
  }

  @Override
  @Transactional
  public void deleteById(String id) {
    commentRepository.deleteById(id);
  }

  private CommentMongo save(String id, String text, String bookId) {
    BookMongo book = bookRepository.findById(bookId)
      .orElseThrow(
        () -> new EntityNotFoundException("Book with id %s not found".formatted(bookId)));
    var comment = new CommentMongo(id, text, book);
    return commentRepository.save(comment);
  }
}
