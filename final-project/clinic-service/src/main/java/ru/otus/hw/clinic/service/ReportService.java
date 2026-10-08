package ru.otus.hw.clinic.service;

import java.util.UUID;
import ru.otus.hw.clinic.dto.ReportDto;
import ru.otus.hw.clinic.dto.ReportPageDto;

public interface ReportService {

  ReportDto findByAppointmentId(UUID appointmentId);

  ReportPageDto findForPatient(UUID patientId, int page, int size);
}
