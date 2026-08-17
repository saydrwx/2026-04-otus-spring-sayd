package ru.otus.hw.commands.jpa;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.jpa.GenreJpaConverter;
import ru.otus.hw.services.jpa.GenreJpaService;

import java.util.stream.Collectors;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class GenreJpaCommands {

  private final GenreJpaService genreService;

  private final GenreJpaConverter genreConverter;

  @ShellMethod(value = "Find all genres", key = "ag")
  public String findAllGenres() {
    return genreService.findAll().stream()
      .map(genreConverter::genreToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Find genres by ids", key = "gbids")
  public String findGenresByIds(Set<Long> ids) {
    return genreService.findAllByIds(ids).stream()
      .map(genreConverter::genreToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }
}
