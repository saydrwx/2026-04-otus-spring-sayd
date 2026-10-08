package ru.otus.hw.web.client;

import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.DocumentsException;
import ru.otus.hw.web.exception.SessionExpiredException;
import ru.otus.hw.web.validation.DocumentFiles;

import static ru.otus.hw.web.exception.DocumentsException.Kind.*;

@Component
public class RestDocumentClient implements DocumentClient {

  private final RestClient http;

  private final DocumentFiles files;

  public RestDocumentClient(@Qualifier("documentRestClient") RestClient http, DocumentFiles files) {
    this.http = http;
    this.files = files;
  }

  @Override
  public DocumentPageDto list(String token, UUID patientId, int page, int size) {
    return read(() -> http.get().uri(
        b -> b.path("/api/patients/{id}/documents").queryParam("page", page).queryParam("size", size)
          .build(patientId))
      .headers(h -> h.setBearerAuth(token)).exchange((request, response) -> {
        check(response.getStatusCode().value(), 200);
        var value = response.bodyTo(DocumentPageDto.class);
        if (value == null || value.page() != page || value.size() != size || value.content() == null
          || value.totalElements() < 0 || value.totalPages() < 0
          || value.content().size() > size || value.totalElements() < value.content().size()
          || value.totalPages() != (value.totalElements() == 0 ? 0
          : ((value.totalElements() - 1) / size + 1))
          || value.content().stream()
          .anyMatch(d -> !files.valid(d) || !patientId.equals(d.patientId()))) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public DocumentDto find(String token, UUID id) {
    return read(() -> http.get().uri("/api/documents/{id}", id).headers(h -> h.setBearerAuth(token))
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 200);
        var value = response.bodyTo(DocumentDto.class);
        if (!files.valid(value) || !id.equals(value.id())) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public DocumentDto upload(String token, DocumentUpload input) {
    if (input == null || input.bytes() == null || input.bytes().length == 0) {
      throw new DocumentsException(INVALID);
    }
    if (input.bytes().length > DocumentFiles.MAX_BYTES) {
      throw new DocumentsException(TOO_LARGE);
    }
    if (!files.filename(input.filename()).equals(input.filename()) || !files.mediaType(
      input.filename()).equals(input.mediaType())) {
      throw new DocumentsException(TYPE);
    }
    var resource = new ByteArrayResource(input.bytes()) {
      @Override
      public String getFilename() {
        return input.filename();
      }
    };
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(input.mediaType()));
    var parts = new LinkedMultiValueMap<String, Object>();
    parts.add("file", new HttpEntity<>(resource, headers));
    return read(() -> http.post().uri("/api/documents").headers(h -> h.setBearerAuth(token))
      .contentType(MediaType.MULTIPART_FORM_DATA).body(parts)
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 201);
        var value = response.bodyTo(DocumentDto.class);
        if (!files.valid(value) || value.sizeBytes() != input.bytes().length || !input.filename()
          .equals(value.filename()) || !input.mediaType().equals(value.mediaType())) {
          throw unavailable();
        }
        return value;
      }));
  }

  @Override
  public DocumentDownload download(String token, UUID id) {
    var metadata = find(token, id);
    return read(
      () -> http.get().uri("/api/documents/{id}/content", id).headers(h -> h.setBearerAuth(token))
        .exchange((request, response) -> {
          check(response.getStatusCode().value(), 200);
          var type = response.getHeaders().getContentType();
          var expected = MediaType.parseMediaType(metadata.mediaType());
          String encoding = response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
          if (type == null || !type.getType().equalsIgnoreCase(expected.getType())
            || !type.getSubtype().equalsIgnoreCase(expected.getSubtype())
            || (encoding != null && !encoding.equalsIgnoreCase("identity"))
            || (response.getHeaders().getContentLength() >= 0
            && response.getHeaders().getContentLength() != metadata.sizeBytes())) {
            throw unavailable();
          }
          byte[] bytes = response.getBody().readNBytes(DocumentFiles.MAX_BYTES + 1);
          if (bytes.length != metadata.sizeBytes() || bytes.length > DocumentFiles.MAX_BYTES) {
            throw unavailable();
          }
          return new DocumentDownload(metadata, bytes);
        }));
  }

  @Override
  public void delete(String token, UUID id) {
    read(() -> http.delete().uri("/api/documents/{id}", id).headers(h -> h.setBearerAuth(token))
      .exchange((request, response) -> {
        check(response.getStatusCode().value(), 204);
        return Boolean.TRUE;
      }));
  }

  private <T> T read(Supplier<T> action) {
    try {
      T result = action.get();
      if (result == null) {
        throw unavailable();
      }
      return result;
    } catch (RestClientException | IllegalArgumentException exception) {
      throw unavailable();
    }
  }

  private void check(int status, int success) {
    if (status == 401) {
      throw new SessionExpiredException();
    }
    if (status == 400 || status == 422) {
      throw new DocumentsException(INVALID);
    }
    if (status == 403) {
      throw new DocumentsException(FORBIDDEN);
    }
    if (status == 404) {
      throw new DocumentsException(NOT_FOUND);
    }
    if (status == 409) {
      throw new DocumentsException(CONFLICT);
    }
    if (status == 413) {
      throw new DocumentsException(TOO_LARGE);
    }
    if (status == 415) {
      throw new DocumentsException(TYPE);
    }
    if (status != success) {
      throw unavailable();
    }
  }

  private DocumentsException unavailable() {
    return new DocumentsException(UNAVAILABLE);
  }
}
