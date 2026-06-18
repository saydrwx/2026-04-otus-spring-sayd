package ru.otus.hw.shell;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.service.TestRunnerService;

@ShellComponent(value = "Testing Commands")
@RequiredArgsConstructor
public class TestingCommands {

  private final TestRunnerService testRunnerService;

  @ShellMethod(value = "Run test", key = { "t", "test" })
  public void runTest() {
    testRunnerService.run();
  }
}
