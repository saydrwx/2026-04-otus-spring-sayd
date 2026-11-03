package ru.otus.hw.web.client;

import ru.otus.hw.web.dto.*;

public interface AdminClient {

  long save(String token, CatalogKind kind, Long id, Object input);

  void delete(String token, CatalogKind kind, long id);

  AppointmentStatisticsDto statistics(String token, StatisticsFilter filter);
}
