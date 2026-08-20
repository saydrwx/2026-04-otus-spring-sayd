package ru.otus.hw.commands.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.mongo.AuthorMongoConverter;
import ru.otus.hw.services.mongo.AuthorMongoService;

import java.util.stream.Collectors;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
@RequiredArgsConstructor
@ShellComponent
public class AuthorMongoCommands {

  private final AuthorMongoService authorService;

  private final AuthorMongoConverter authorConverter;

  @ShellMethod(value = "Find all authors", key = "aam")
  public String findAllAuthors() {
    return authorService.findAll().stream()
      .map(authorConverter::authorToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Find author by id", key = "abidm")
  public String findAuthorById(String id) {
    return authorService.findById(id)
      .map(authorConverter::authorToString)
      .orElse("Author with id %s not found".formatted(id));
  }
}
