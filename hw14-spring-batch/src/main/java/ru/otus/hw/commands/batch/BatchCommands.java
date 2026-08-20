package ru.otus.hw.commands.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@SuppressWarnings("unused")
@RequiredArgsConstructor
@ShellComponent
public class BatchCommands {

  private final JobLauncher jobLauncher;

  private final Job migrationJob;

  @ShellMethod(value = "startMigrationJob", key = "mig")
  public void startMigration() throws Exception {
    jobLauncher.run(migrationJob, new JobParameters());
  }
}
