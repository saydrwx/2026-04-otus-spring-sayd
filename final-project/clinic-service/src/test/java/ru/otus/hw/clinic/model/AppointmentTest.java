package ru.otus.hw.clinic.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.otus.hw.clinic.exception.AppointmentConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

  private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-04T09:00:00+03:00");

  private Appointment bookedAppointment() {
    return new Appointment(new Patient(UUID.randomUUID()), 1, 2, START, START.plusMinutes(30),
      Instant.parse("2026-11-02T06:00:00Z"));
  }

  @Test
  void cancellationIsAllowedStrictlyBeforeStart() {
    var appointment = bookedAppointment();
    UUID actor = UUID.randomUUID();
    var justBefore = START.toInstant().minusNanos(1);
    appointment.cancel(actor, justBefore);
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    assertThat(appointment.getCancelledAt()).isEqualTo(justBefore);
    assertThat(appointment.getCancelledBy()).isEqualTo(actor);
  }

  @Test
  void cancellationAtOrAfterStartDoesNotChangeTheBooking() {
    for (Instant now : java.util.List.of(START.toInstant(), START.toInstant().plusNanos(1))) {
      var appointment = bookedAppointment();
      assertThatThrownBy(() -> appointment.cancel(UUID.randomUUID(), now))
        .isInstanceOf(AppointmentConflictException.class);
      assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
      assertThat(appointment.getCancelledAt()).isNull();
    }
  }

  @Test
  void repeatedCancellationKeepsTheOriginalActorAndTimestamp() {
    var appointment = bookedAppointment();
    UUID first = UUID.randomUUID();
    Instant cancelledAt = START.toInstant().minusSeconds(10);
    appointment.cancel(first, cancelledAt);
    appointment.cancel(UUID.randomUUID(), START.toInstant().plusSeconds(10));
    assertThat(appointment.getCancelledAt()).isEqualTo(cancelledAt);
    assertThat(appointment.getCancelledBy()).isEqualTo(first);
  }

  @Test
  void outcomeAtEndIsAllowedAndEarlierOutcomeIsRejected() {
    var appointment = bookedAppointment();
    UUID doctor = UUID.randomUUID();
    Instant end = START.plusMinutes(30).toInstant();
    assertThatThrownBy(() -> appointment.complete(doctor, end.minusNanos(1)))
      .isInstanceOf(AppointmentConflictException.class);
    appointment.complete(doctor, end);
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    assertThat(appointment.getOutcomeAt()).isEqualTo(end);
    assertThatThrownBy(() -> appointment.markNoShow(doctor, end))
      .isInstanceOf(AppointmentConflictException.class);
  }

  @Test
  void cancelledAndNoShowAppointmentsCannotBeCompleted() {
    UUID doctor = UUID.randomUUID();
    var cancelled = bookedAppointment();
    cancelled.cancel(UUID.randomUUID(), START.toInstant().minusSeconds(1));
    assertThatThrownBy(() -> cancelled.complete(doctor, START.plusMinutes(30).toInstant()))
      .isInstanceOf(AppointmentConflictException.class);
    var noShow = bookedAppointment();
    noShow.markNoShow(doctor, START.plusMinutes(30).toInstant());
    assertThatThrownBy(() -> noShow.complete(doctor, START.plusMinutes(30).toInstant()))
      .isInstanceOf(AppointmentConflictException.class);
  }
}
