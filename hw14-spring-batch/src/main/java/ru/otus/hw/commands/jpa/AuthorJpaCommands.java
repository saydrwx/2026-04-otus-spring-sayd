package ru.otus.hw.commands.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.converters.jpa.AuthorJpaConverter;
import ru.otus.hw.services.jpa.AuthorJpaService;

import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RequiredArgsConstructor
@ShellComponent
public class AuthorJpaCommands {

  private final AuthorJpaService authorService;

  private final AuthorJpaConverter authorConverter;

  @ShellMethod(value = "Find all authors", key = "aa")
  public String findAllAuthors() {
    return authorService.findAll().stream()
      .map(authorConverter::authorToString)
      .collect(Collectors.joining("," + System.lineSeparator()));
  }

  @ShellMethod(value = "Find author by id", key = "abid")
  public String findAuthorById(long id) {
    return authorService.findById(id)
      .map(authorConverter::authorToString)
      .orElse("Author with id %d not found".formatted(id));
  }
}
