package ru.otus.hw.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.web.client.AdminClient;
import ru.otus.hw.web.dto.AppointmentStatisticsDto;
import ru.otus.hw.web.dto.CatalogKind;
import ru.otus.hw.web.dto.StatisticsFilter;

@RequiredArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {

  private final AdminClient client;

  public long save(String token, CatalogKind kind, Long id, Object input) {
    return client.save(token, kind, id, input);
  }

  public void delete(String token, CatalogKind kind, long id) {
    client.delete(token, kind, id);
  }

  public AppointmentStatisticsDto statistics(String token, StatisticsFilter filter) {
    return client.statistics(token, filter);
  }
}
