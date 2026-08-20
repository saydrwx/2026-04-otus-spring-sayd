package ru.otus.hw.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@RequiredArgsConstructor
public class JobConfig {

  private final JobRepository jobRepository;

  @Bean
  public Job migrationJob(
    Step cleanUpStep,
    Flow authorGenreFlow,
    Step bookMigration,
    Step commentMigration
  ) {
    return new JobBuilder("migration", jobRepository)
      .incrementer(new RunIdIncrementer())
      .flow(cleanUpStep)
      .next(authorGenreFlow)
      .next(bookMigration)
      .next(commentMigration)
      .end()
      .build();
  }

  @Bean
  public Flow authorGenreFlow(Flow authorFlow, Flow genreFlow) {
    return new FlowBuilder<Flow>("authorGenreFlow")
      .split(taskExecutor())
      .add(authorFlow, genreFlow)
      .build();
  }

  @Bean
  public Flow authorFlow(Step authorMigration) {
    return new FlowBuilder<SimpleFlow>("authorFlow")
      .start(authorMigration)
      .build();
  }

  @Bean
  public Flow genreFlow(Step genreMigration) {
    return new FlowBuilder<SimpleFlow>("genreFlow")
      .start(genreMigration)
      .build();
  }

  @Bean
  public TaskExecutor taskExecutor() {
    return new SimpleAsyncTaskExecutor("spring_batch");
  }
}
