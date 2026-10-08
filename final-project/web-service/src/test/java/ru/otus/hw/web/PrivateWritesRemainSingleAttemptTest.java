package ru.otus.hw.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.otus.hw.web.client.*;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PrivateWritesRemainSingleAttemptTest {

  @Test
  void directoryCreateUpdateAndDeleteHaveNoTransientRetryPolicy() {
    for (String operation : java.util.List.of("create", "update", "delete")) {
      var builder = TestRestClients.builder("http://directory.test");
      var server = MockRestServiceServer.bindTo(builder).build();
      var http = builder.build();
      var client = new RestAdminClient(http, http);
      server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
      assertThatThrownBy(() -> {
        switch (operation) {
          case "create" -> client.save("token", CatalogKind.HOSPITALS, null,
            new HospitalAdminForm.Input("Name", "Address"));
          case "update" -> client.save("token", CatalogKind.HOSPITALS, 1L,
            new HospitalAdminForm.Input("Name", "Address"));
          default -> client.delete("token", CatalogKind.HOSPITALS, 1);
        }
      }).isInstanceOf(AdminException.class);
      server.verify();
    }
  }

  @Test
  void appointmentBookingAndCancellationHaveNoTransientRetryPolicy() {
    for (boolean booking : new boolean[]{true, false}) {
      var builder = TestRestClients.builder("http://clinic.test");
      var server = MockRestServiceServer.bindTo(builder).build();
      var client = new RestAppointmentClient(builder.build());
      server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
      assertThatThrownBy(() -> {
        if (booking) {
          client.book("token", new BookingRequest(1, AppointmentFixtures.START));
        } else {
          client.cancel("token", AppointmentFixtures.ID);
        }
      })
        .isInstanceOf(AppointmentsException.class);
      server.verify();
    }
  }
}
