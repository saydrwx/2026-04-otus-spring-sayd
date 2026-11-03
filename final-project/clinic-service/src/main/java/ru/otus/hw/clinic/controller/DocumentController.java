package ru.otus.hw.clinic.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import ru.otus.hw.clinic.dto.DocumentDto;
import ru.otus.hw.clinic.dto.DocumentPageDto;
import ru.otus.hw.clinic.exception.InvalidDocumentException;
import ru.otus.hw.clinic.service.DocumentService;
import ru.otus.hw.clinic.storage.DocumentDownload;

@RequiredArgsConstructor
@RestController
public class DocumentController {

  private final DocumentService documentService;

  @PostMapping(value = "/api/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DocumentDto> upload(@RequestPart("file") MultipartFile file,
    MultipartHttpServletRequest request) {
    if (request.getMultiFileMap().values().stream().mapToInt(java.util.List::size).sum() != 1) {
      throw new InvalidDocumentException(HttpStatus.BAD_REQUEST, "Upload exactly one file");
    }
    DocumentDto result = documentService.upload(file);
    return ResponseEntity.created(URI.create("/api/documents/" + result.id())).body(result);
  }

  @GetMapping("/api/patients/{patientId}/documents")
  public DocumentPageDto list(
    @PathVariable UUID patientId,
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return documentService.list(patientId, page, size);
  }

  @GetMapping("/api/documents/{id}")
  public DocumentDto find(@PathVariable UUID id) {
    return documentService.find(id);
  }

  @GetMapping("/api/documents/{id}/content")
  public ResponseEntity<byte[]> download(@PathVariable UUID id) {
    DocumentDownload result = documentService.download(id);
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(result.metadata().mediaType()))
      .contentLength(result.bytes().length)
      .cacheControl(CacheControl.noStore().cachePrivate())
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
        .filename(result.metadata().filename(), StandardCharsets.UTF_8).build().toString())
      .header("Content-Security-Policy", "sandbox; default-src 'none'")
      .header("X-Content-Type-Options", "nosniff")
      .body(result.bytes());
  }

  @DeleteMapping("/api/documents/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    documentService.delete(id);
  }
}
