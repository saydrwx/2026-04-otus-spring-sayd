package ru.otus.hw.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.exception.AuthorNotFoundException;
import ru.otus.hw.mapper.AuthorMapper;
import ru.otus.hw.model.Author;
import ru.otus.hw.repository.AuthorRepository;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AuthorServiceImpl implements AuthorService {

  private final AuthorRepository authorRepository;

  private final AuthorMapper authorMapper;

  private final AclServiceWrapperService aclService;

  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  @Override
  public AuthorDto findById(long id) {
    return authorMapper.toDto(findAuthor(id));
  }

  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  @Override
  public List<AuthorDto> findAll() {
    return authorRepository.findAllByOrderByFullNameAsc().stream()
      .map(authorMapper::toDto)
      .toList();
  }

  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  @Override
  @Transactional
  public AuthorDto create(AuthorForm form) {
    Author author = authorMapper.toEntity(form);
    Author savedAuthor = authorRepository.save(author);
    aclService.createPermission(savedAuthor, BasePermission.READ, BasePermission.WRITE,
      BasePermission.DELETE);
    return authorMapper.toDto(savedAuthor);
  }

  @PreAuthorize("hasPermission(#id, 'ru.otus.hw.model.Author', 'WRITE')")
  @Override
  @Transactional
  public AuthorDto update(long id, AuthorForm form) {
    Author author = findAuthor(id);
    authorMapper.updateEntity(form, author);
    return authorMapper.toDto(author);
  }

  @PreAuthorize("hasPermission(#id, 'ru.otus.hw.model.Author', 'DELETE')")
  @Override
  @Transactional
  public void deleteById(long id) {
    Author author = findAuthor(id);
    aclService.deletePermissions(id, Author.class);
    authorRepository.delete(author);
  }

  private Author findAuthor(long id) {
    return authorRepository.findById(id).orElseThrow(() -> new AuthorNotFoundException(id));
  }
}
