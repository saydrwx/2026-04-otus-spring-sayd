package ru.otus.hw.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.otus.hw.web.dto.DocumentDto;
import ru.otus.hw.web.exception.DocumentsException;
import ru.otus.hw.web.validation.DocumentFiles;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ru.otus.hw.web.DocumentFixtures.*;

class DocumentFilesTest {

  final DocumentFiles files = new DocumentFiles();

  @Test
  void matchesBackendLimitAndNormalizesGenericMimeAndUnicodeFilename() {
    var result = files.prepare(
      new MockMultipartFile("file", "re\u0301sultat.PDF", "application/octet-stream", PDF));
    assertThat(result.filename()).isEqualTo("résultat.PDF");
    assertThat(result.mediaType()).isEqualTo("application/pdf");
    assertThat(result.bytes()).isEqualTo(PDF);
    assertThat(files.prepare(new MockMultipartFile("file", "limit.pdf", "application/pdf",
      new byte[DocumentFiles.MAX_BYTES])).bytes()).hasSize(DocumentFiles.MAX_BYTES);
  }

  @Test
  void rejectsPathsControlCharactersEmptyFilesAndTypeMismatch() {
    for (String name : new String[]{"../lab.pdf", "C:\\lab.pdf", "bad\r\nheader.pdf",
      "x/scan.pdf"}) {
      assertThatThrownBy(() -> files.prepare(
        new MockMultipartFile("file", name, "application/pdf", PDF))).isInstanceOf(
        DocumentsException.class);
    }
    assertThatThrownBy(() -> files.prepare(
      new MockMultipartFile("file", "lab.pdf", "image/png", PDF))).isInstanceOfSatisfying(
      DocumentsException.class, e -> assertThat(e.kind()).isEqualTo(DocumentsException.Kind.TYPE));
    assertThatThrownBy(() -> files.prepare(
      new MockMultipartFile("file", "lab.pdf", "application/pdf", new byte[0]))).isInstanceOf(
      DocumentsException.class);
  }

  @Test
  void boundedReadRejectsASpoofedSizeWithoutReadingAnUnboundedStream() throws IOException {
    var file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1L);
    when(file.getOriginalFilename()).thenReturn("lab.pdf");
    when(file.getContentType()).thenReturn("application/pdf");
    var input = new ByteArrayInputStream(new byte[DocumentFiles.MAX_BYTES + 100]);
    when(file.getInputStream()).thenReturn(input);
    assertThatThrownBy(() -> files.prepare(file)).isInstanceOfSatisfying(DocumentsException.class,
      e -> assertThat(e.kind()).isEqualTo(DocumentsException.Kind.TOO_LARGE));
    assertThat(input.available()).isEqualTo(99);
  }

  @Test
  void validatesMetadataBeforeUsingItInDownloadHeaders() {
    assertThat(files.valid(DOC)).isTrue();
    assertThat(files.valid(
      new DocumentDto(DOC.id(), DOC.patientId(), "bad\r\nheader.pdf", "application/pdf", PDF.length,
        DOC.uploadedAt()))).isFalse();
    assertThat(files.valid(
      new DocumentDto(DOC.id(), DOC.patientId(), "scan.svg", "image/svg+xml", PDF.length,
        DOC.uploadedAt()))).isFalse();
  }
}
