package ru.otus.hw.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import ru.otus.hw.web.dto.*;

final class DocumentFixtures {

  private DocumentFixtures() {
  }

  static final UUID DOC_ID = UUID.fromString("50a79bc9-7e49-44e9-b581-b171c6a100a0");
  static final byte[] PDF = "%PDF-1.7\nDocument\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
  static final DocumentDto DOC = new DocumentDto(DOC_ID, AppointmentFixtures.PATIENT, "lab.pdf",
    "application/pdf", PDF.length, Instant.parse("2030-01-03T09:30:00Z"));
  static final String DOC_JSON = """
    {"id":"50a79bc9-7e49-44e9-b581-b171c6a100a0","patientId":"a1685736-c25a-4f19-9693-a491084d5bc9",
     "filename":"lab.pdf","mediaType":"application/pdf","sizeBytes":24,"uploadedAt":"2030-01-03T09:30:00Z"}
    """;
}
