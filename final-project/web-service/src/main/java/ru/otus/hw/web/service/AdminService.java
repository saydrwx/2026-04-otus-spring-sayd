package ru.otus.hw.web.service;

import ru.otus.hw.web.dto.AppointmentStatisticsDto;
import ru.otus.hw.web.dto.CatalogKind;
import ru.otus.hw.web.dto.StatisticsFilter;

public interface AdminService {

  long save(String token, CatalogKind kind, Long id, Object input);

  void delete(String token, CatalogKind kind, long id);

  AppointmentStatisticsDto statistics(String token, StatisticsFilter filter);
}
