package ru.otus.hw.actuator;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import ru.otus.hw.model.Book;
import ru.otus.hw.repository.BookRepository;

@RequiredArgsConstructor
@Component("libraryHealthCheck")
public class CustomHealthIndicator implements HealthIndicator {

  private final BookRepository bookRepository;

  private final MessageSource messageSource;

  @Override
  public Health health() {
    Locale locale = LocaleContextHolder.getLocale();

    List<Book> books = bookRepository.findAll();
    if (books.isEmpty()) {
      return Health.down()
        .withDetail("info",
          messageSource.getMessage("actuator.health.library-empty", null , locale))
        .build();
    } else {
      return Health.up()
        .withDetail("info",
          messageSource.getMessage("actuator.health.library-not-empty", null , locale))
        .build();
    }
  }
}
