package ru.otus.hw.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.exception.GenreNotFoundException;
import ru.otus.hw.mapper.GenreMapper;
import ru.otus.hw.model.Genre;
import ru.otus.hw.repository.GenreRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {

  private final GenreRepository genreRepository;

  private final GenreMapper genreMapper;

  private final AclServiceWrapperService aclService;

  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  @Override
  public GenreDto findById(long id) {
    return genreMapper.toDto(findGenre(id));
  }

  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  @Override
  public List<GenreDto> findAll() {
    return genreRepository.findAllByOrderByNameAsc().stream()
        .map(genreMapper::toDto)
        .toList();
  }

  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  @Override
  @Transactional
  public GenreDto create(GenreForm form) {
    Genre genre = genreMapper.toEntity(form);
    Genre savedGenre = genreRepository.save(genre);
    aclService.createPermission(savedGenre, BasePermission.READ, BasePermission.WRITE,
      BasePermission.DELETE);
    return genreMapper.toDto(savedGenre);
  }

  @PreAuthorize("hasPermission(#id, 'ru.otus.hw.model.Genre', 'WRITE')")
  @Override
  @Transactional
  public GenreDto update(long id, GenreForm form) {
    Genre genre = findGenre(id);
    genreMapper.updateEntity(form, genre);
    return genreMapper.toDto(genre);
  }

  @PreAuthorize("hasPermission(#id, 'ru.otus.hw.model.Genre', 'DELETE')")
  @Override
  @Transactional
  public void deleteById(long id) {
    Genre genre = findGenre(id);
    aclService.deletePermissions(id, Genre.class);
    genreRepository.delete(genre);
  }

  private Genre findGenre(long id) {
    return genreRepository.findById(id)
        .orElseThrow(() -> new GenreNotFoundException(id));
  }
}
