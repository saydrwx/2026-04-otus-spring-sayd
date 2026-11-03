package ru.otus.hw.clinic.service;

import java.util.UUID;
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportInputDto;

public interface VisitService {

  ReportDto submitReport(UUID appointmentId, ReportInputDto input);

  void markNoShow(UUID appointmentId);
}
