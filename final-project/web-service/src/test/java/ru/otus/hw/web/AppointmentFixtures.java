package ru.otus.hw.web;

import java.time.*;
import java.util.List;
import java.util.UUID;
import ru.otus.hw.web.dto.*;

final class AppointmentFixtures {

  private AppointmentFixtures() {
  }

  static final UUID ID = UUID.fromString("31558885-0e53-4f91-8aa5-d65eedf35c41");
  static final UUID PATIENT = UUID.fromString("a1685736-c25a-4f19-9693-a491084d5bc9");
  static final Instant NOW = Instant.parse("2030-01-01T08:00:00Z");
  static final OffsetDateTime START = OffsetDateTime.parse("2030-01-03T12:00:00+03:00");
  static final HospitalDto HOSPITAL = new HospitalDto(2, "Central Hospital", "Main Street");
  static final DoctorDto DOCTOR = new DoctorDto(1, "Alex", "Smith", "University", 10,
    "doctor@example.test", HOSPITAL, List.of(new SpecialtyDto(3, "Cardiology")));
  static final AppointmentDto BOOKED = new AppointmentDto(ID, PATIENT, 1, 2, START,
    START.plusMinutes(30), AppointmentStatus.BOOKED, NOW, null, null);

  static AvailabilityDto available() {
    return new AvailabilityDto(1, 2, START.toLocalDate(), "+03:00", 30,
      NOW.atOffset(ZoneOffset.ofHours(3)).toLocalDate().plusDays(1).atStartOfDay()
        .atOffset(ZoneOffset.ofHours(3)),
      OffsetDateTime.ofInstant(NOW.plusSeconds(14 * 86400), ZoneOffset.ofHours(3)),
      List.of(new SlotDto(START, START.plusMinutes(30))));
  }

  static final String BODY = """
    {"id":"31558885-0e53-4f91-8aa5-d65eedf35c41","patientId":"a1685736-c25a-4f19-9693-a491084d5bc9",
     "doctorId":1,"hospitalId":2,"start":"2030-01-03T12:00:00+03:00","end":"2030-01-03T12:30:00+03:00",
     "status":"BOOKED","bookedAt":"2030-01-01T08:00:00Z","cancelledAt":null,"outcomeAt":null}
    """;
  static final String AVAILABILITY = """
    {"doctorId":1,"hospitalId":2,"date":"2030-01-03","timezone":"+03:00","slotDurationMinutes":30,
     "earliestStart":"2030-01-02T00:00:00+03:00","latestStart":"2030-01-15T11:00:00+03:00",
     "slots":[{"start":"2030-01-03T12:00:00+03:00","end":"2030-01-03T12:30:00+03:00"}]}
    """;
}
