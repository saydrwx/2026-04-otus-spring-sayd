package ru.otus.hw.commands.mongo;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.mongo.GenreMongoConverter;
import ru.otus.hw.services.mongo.GenreMongoService;

import java.util.stream.Collectors;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class GenreMongoCommands {

  private final GenreMongoService genreService;

  private final GenreMongoConverter genreConverter;

  @ShellMethod(value = "Find all genres", key = "agm")
  public String findAllGenres() {
    return genreService.findAll().stream()
      .map(genreConverter::genreToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Find genres by ids", key = "gbidsm")
  public String findGenresByIds(Set<String> ids) {
    return genreService.findAllByIds(ids).stream()
      .map(genreConverter::genreToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }
}
