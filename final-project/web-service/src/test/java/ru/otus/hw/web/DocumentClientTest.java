package ru.otus.hw.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.otus.hw.web.client.RestDocumentClient;
import ru.otus.hw.web.dto.DocumentUpload;
import ru.otus.hw.web.exception.DocumentsException;
import ru.otus.hw.web.exception.SessionExpiredException;
import ru.otus.hw.web.validation.DocumentFiles;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static ru.otus.hw.web.AppointmentFixtures.PATIENT;
import static ru.otus.hw.web.DocumentFixtures.*;

class DocumentClientTest {

  MockRestServiceServer server;
  RestDocumentClient client;

  @BeforeEach
  void setup() {
    var builder = TestRestClients.builder("http://clinic.test");
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RestDocumentClient(builder.build(), new DocumentFiles());
  }

  @Test
  void listAndMetadataHaveCurrentBearerAndNoCookies() {
    server.expect(
        requestTo("http://clinic.test/api/patients/" + PATIENT + "/documents?page=1&size=20"))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(headerDoesNotExist("Cookie")).andRespond(withSuccess("""
        {"content":[%s],"page":1,"size":20,"totalElements":21,"totalPages":2}
        """.formatted(DOC_JSON), MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andRespond(withSuccess(DOC_JSON, MediaType.APPLICATION_JSON));
    assertThat(client.list("current-token", PATIENT, 1, 20).content()).hasSize(1);
    assertThat(client.find("current-token", DOC_ID).filename()).isEqualTo("lab.pdf");
    server.verify();
  }

  @Test
  void uploadIsOneMultipartFileWithoutPatientIdCsrfOrCookies() {
    server.expect(requestTo("http://clinic.test/api/documents")).andExpect(method(HttpMethod.POST))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(headerDoesNotExist("Cookie")).andExpect(request -> {
        assertThat(request.getHeaders().getContentType()
          .isCompatibleWith(MediaType.MULTIPART_FORM_DATA)).isTrue();
        String body = new String(((MockClientHttpRequest) request).getBodyAsBytes(),
          StandardCharsets.ISO_8859_1);
        assertThat(body).contains("name=\"file\"", "filename=\"lab.pdf\"", "application/pdf",
          "%PDF-1.7").doesNotContain("patientId", "_csrf");
        assertThat(body.split("name=\"file\"", -1)).hasSize(2);
      }).andRespond(
        withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(DOC_JSON));
    assertThat(client.upload("current-token", new DocumentUpload("lab.pdf", "application/pdf", PDF))
      .id()).isEqualTo(DOC_ID);
    server.verify();
  }

  @Test
  void downloadAuthorizesMetadataAndContentAndReadsOnlyValidatedBytes() {
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andRespond(withSuccess(DOC_JSON, MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID + "/content"))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andExpect(headerDoesNotExist("Cookie"))
      .andRespond(withSuccess(PDF, MediaType.APPLICATION_PDF).header("Content-Disposition",
        "inline; filename=untrusted-name.pdf"));
    var result = client.download("current-token", DOC_ID);
    assertThat(result.bytes()).isEqualTo(PDF);
    assertThat(result.metadata().filename()).isEqualTo("lab.pdf");
    server.verify();
  }

  @Test
  void metadataDenialDoesNotFetchBytesAndContentRevocationRemains404() {
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
      .andRespond(withSuccess(DOC_JSON, MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID + "/content"))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> client.download("token", DOC_ID)).isInstanceOfSatisfying(
        DocumentsException.class,
        e -> assertThat(e.kind()).isEqualTo(DocumentsException.Kind.NOT_FOUND));
    }
    server.verify();
  }

  @Test
  void truncatedWrongTypeAndOversizedContentDoNotBecomeDownloads() {
    for (int i = 0; i < 3; i++) {
      server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
        .andRespond(withSuccess(DOC_JSON, MediaType.APPLICATION_JSON));
      var expected = server.expect(
        requestTo("http://clinic.test/api/documents/" + DOC_ID + "/content"));
      if (i == 0) {
        expected.andRespond(withSuccess(new byte[1], MediaType.APPLICATION_PDF));
      } else if (i == 1) {
        expected.andRespond(withSuccess(PDF, MediaType.TEXT_HTML));
      } else {
        expected.andRespond(
          withSuccess(new byte[DocumentFiles.MAX_BYTES + 1], MediaType.APPLICATION_PDF));
      }
    }
    for (int i = 0; i < 3; i++) {
      assertUnavailable(() -> client.download("token", DOC_ID));
    }
    server.verify();
  }

  @Test
  void errorsRetainStatusWithoutLeakingProviderBody() {
    for (var status : List.of(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND,
      HttpStatus.CONFLICT, HttpStatus.PAYLOAD_TOO_LARGE, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      HttpStatus.SERVICE_UNAVAILABLE)) {
      server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
        .andRespond(withStatus(status).body("private-storage-path-and-patient-data"));
    }
    assertThatThrownBy(() -> client.find("token", DOC_ID)).isInstanceOf(
      SessionExpiredException.class);
    for (var kind : List.of(DocumentsException.Kind.FORBIDDEN, DocumentsException.Kind.NOT_FOUND,
      DocumentsException.Kind.CONFLICT, DocumentsException.Kind.TOO_LARGE,
      DocumentsException.Kind.TYPE, DocumentsException.Kind.UNAVAILABLE)) {
      assertThatThrownBy(() -> client.find("token", DOC_ID)).isInstanceOfSatisfying(
          DocumentsException.class, e -> assertThat(e.kind()).isEqualTo(kind))
        .hasMessageNotContaining("private-storage-path-and-patient-data");
    }
    server.verify();
  }

  @Test
  void deleteUsesOneBackendDeleteAndNeverRetriesAFailure() {
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
      .andExpect(method(HttpMethod.DELETE))
      .andExpect(header("Authorization", "Bearer current-token"))
      .andRespond(withServerError());
    assertUnavailable(() -> {
      client.delete("current-token", DOC_ID);
      return null;
    });
    server.verify();
  }

  @Test
  void wrongPatientOrMalformedMetadataCannotBecomeAnEmptyCollection() {
    UUID other = UUID.fromString("4f63cebf-5167-46c4-aad0-bd3ae535dbf5");
    server.expect(
        requestTo("http://clinic.test/api/patients/" + other + "/documents?page=0&size=20"))
      .andRespond(withSuccess("""
        {"content":[%s],"page":0,"size":20,"totalElements":1,"totalPages":1}
        """.formatted(DOC_JSON), MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://clinic.test/api/documents/" + DOC_ID))
      .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    assertUnavailable(() -> client.list("token", other, 0, 20));
    assertUnavailable(() -> client.find("token", DOC_ID));
    server.verify();
  }

  @Test
  void diagnosticStringsDoNotPrintFilenameOrBytes() {
    assertThat(DOC.toString()).doesNotContain("lab.pdf");
    assertThat(
      new DocumentUpload("private-name.pdf", "application/pdf", PDF).toString()).doesNotContain(
      "private-name.pdf", "%PDF");
  }

  void assertUnavailable(java.util.concurrent.Callable<?> action) {
    assertThatThrownBy(action::call).isInstanceOfSatisfying(DocumentsException.class,
      e -> assertThat(e.kind()).isEqualTo(DocumentsException.Kind.UNAVAILABLE));
  }
}
