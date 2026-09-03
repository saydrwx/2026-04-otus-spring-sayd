package ru.otus.hw.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.exception.BookNotFoundException;
import ru.otus.hw.exception.CommentNotFoundException;
import ru.otus.hw.mapper.CommentMapper;
import ru.otus.hw.model.Book;
import ru.otus.hw.model.Comment;
import ru.otus.hw.repository.BookRepository;
import ru.otus.hw.repository.CommentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

  private final CommentRepository commentRepository;

  private final BookRepository bookRepository;

  private final CommentMapper commentMapper;

  @Override
  public CommentDto findById(long commentId) {
    return commentMapper.toDto(findComment(commentId));
  }

  @Override
  public List<CommentDto> findAllByBookId(long bookId) {
    return commentRepository.findAllByBookIdOrderByIdAsc(bookId).stream()
        .map(commentMapper::toDto)
        .toList();
  }

  @Override
  @Transactional
  public CommentDto create(long bookId, CommentForm form) {
    Book book = findBook(bookId);
    Comment comment = commentMapper.toEntity(form, book);
    return commentMapper.toDto(commentRepository.save(comment));
  }

  @Override
  @Transactional
  public CommentDto update(long commentId, CommentForm form) {
    Comment comment = findComment(commentId);
    commentMapper.updateEntity(form, comment);
    return commentMapper.toDto(comment);
  }

  @Override
  @Transactional
  public void deleteById(long commentId) {
    Comment comment = findComment(commentId);
    commentRepository.delete(comment);
  }

  private Comment findComment(long commentId) {
    return commentRepository.findById(commentId)
      .orElseThrow(() -> new CommentNotFoundException(commentId));
  }

  private Book findBook(long bookId) {
    return bookRepository.findById(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));
  }

}
