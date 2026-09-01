package ru.otus.hw.service;

import java.util.List;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.CommentForm;

public interface CommentService {

  CommentDto findById(long commentId);

  List<CommentDto> findAllByBookId(long bookId);

  CommentDto create(long bookId, CommentForm form);

  CommentDto update(long commentId, CommentForm form);

  void deleteById(long commentId);
}
