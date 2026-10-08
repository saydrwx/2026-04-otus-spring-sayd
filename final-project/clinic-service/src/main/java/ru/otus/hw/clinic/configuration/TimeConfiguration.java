package ru.otus.hw.clinic.configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

  public static final ZoneOffset HOSPITAL_OFFSET = ZoneOffset.ofHours(3);

  public static final Duration SLOT_DURATION = Duration.ofMinutes(30);

  @Bean
  Clock clinicClock() {
    return Clock.system(HOSPITAL_OFFSET);
  }
}
