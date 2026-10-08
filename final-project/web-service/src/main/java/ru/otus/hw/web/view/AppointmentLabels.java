package ru.otus.hw.web.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.web.dto.AppointmentDto;
import ru.otus.hw.web.dto.ReportDto;
import ru.otus.hw.web.dto.DoctorSearchInput;
import ru.otus.hw.web.exception.DirectoryUnavailableException;
import ru.otus.hw.web.service.DirectoryService;

@RequiredArgsConstructor
@Component
public class AppointmentLabels {

  private final DirectoryService directory;

  public Labels forAppointments(List<AppointmentDto> values) {
    return labels(!values.isEmpty());
  }

  public Labels forReports(List<ReportDto> values) {
    return labels(!values.isEmpty());
  }

  private Labels labels(boolean haveRows) {
    var doctors = new HashMap<Long, String>();
    var hospitals = new HashMap<Long, String>();
    if (haveRows) {
      try {
        directory.doctors(new DoctorSearchInput(null, List.of()))
          .forEach(d -> doctors.put(d.id(), d.firstName() + " " + d.lastName()));
        directory.hospitals().forEach(h -> hospitals.put(h.id(), h.name()));
      } catch (DirectoryUnavailableException ignored) {
        System.out.println("DirectoryUnavailableException ignored");
      }
    }
    return new Labels(Map.copyOf(doctors), Map.copyOf(hospitals));
  }

  public record Labels(Map<Long, String> doctors, Map<Long, String> hospitals) {

  }
}
