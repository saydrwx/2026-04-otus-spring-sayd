package ru.otus.hw.web.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.otus.hw.web.view.ClinicTimes;

@Configuration(proxyBeanMethods = false)
public class AppointmentTimeConfiguration {

  @Bean
  Clock appointmentClock() {
    return Clock.system(ClinicTimes.OFFSET);
  }
}
