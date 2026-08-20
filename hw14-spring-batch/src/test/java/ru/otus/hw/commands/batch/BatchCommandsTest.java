package ru.otus.hw.commands.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

class BatchCommandsTest {

  private JobLauncher jobLauncher;

  private Job migrationJob;

  private BatchCommands commands;

  @BeforeEach
  void setUp() {
    jobLauncher = mock(JobLauncher.class);
    migrationJob = mock(Job.class);
    commands = new BatchCommands(jobLauncher, migrationJob);
  }

  @Test
  void startMigrationShouldLaunchConfiguredJobWithEmptyParameters() throws Exception {
    when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
      .thenReturn(mock(JobExecution.class));

    commands.startMigration();

    var parameters = org.mockito.ArgumentCaptor.forClass(JobParameters.class);
    verify(jobLauncher).run(org.mockito.ArgumentMatchers.same(migrationJob), parameters.capture());
    assertThat(parameters.getValue().getParameters()).isEmpty();
  }

  @Test
  void startMigrationShouldPropagateLauncherFailure() throws Exception {
    var failure = new JobExecutionAlreadyRunningException("Migration is already running");
    when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenThrow(failure);

    assertThatThrownBy(commands::startMigration).isSameAs(failure);
  }
}
