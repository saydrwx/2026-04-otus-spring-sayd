package ru.otus.hw.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

class JobConfigTest {

  private JobConfig config;

  @BeforeEach
  void setUp() {
    config = new JobConfig(mock(JobRepository.class));
  }

  @Test
  void shouldBuildNamedAuthorAndGenreFlowsAndParallelSplit() {
    var authorStep = namedStep("authorMigrationStep");
    var genreStep = namedStep("genreMigrationStep");

    var authorFlow = config.authorFlow(authorStep);
    var genreFlow = config.genreFlow(genreStep);
    var split = config.authorGenreFlow(authorFlow, genreFlow);

    assertThat(authorFlow.getName()).isEqualTo("authorFlow");
    assertThat(genreFlow.getName()).isEqualTo("genreFlow");
    assertThat(split.getName()).isEqualTo("authorGenreFlow");
  }

  @Test
  void shouldBuildMigrationJobWithRunIdIncrementer() {
    var cleanup = namedStep("cleanUpStep");
    var authorFlow = config.authorFlow(namedStep("authorMigrationStep"));
    var genreFlow = config.genreFlow(namedStep("genreMigrationStep"));
    var split = config.authorGenreFlow(authorFlow, genreFlow);
    var book = namedStep("bookMigrationStep");
    var comment = namedStep("commentMigrationStep");

    var job = config.migrationJob(cleanup, split, book, comment);

    assertThat(job.getName()).isEqualTo("migration");
    assertThat(job.getJobParametersIncrementer()).isInstanceOf(RunIdIncrementer.class);
  }

  @Test
  void taskExecutorShouldUseARecognizableBatchThreadPrefix() {
    var executor = config.taskExecutor();

    assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
    assertThat(((SimpleAsyncTaskExecutor) executor).getThreadNamePrefix())
      .isEqualTo("spring_batch");
  }

  private static Step namedStep(String name) {
    var step = mock(Step.class);
    when(step.getName()).thenReturn(name);
    return step;
  }
}
