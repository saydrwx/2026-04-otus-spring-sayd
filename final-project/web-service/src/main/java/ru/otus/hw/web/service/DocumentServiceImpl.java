package ru.otus.hw.web.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.web.client.DocumentClient;
import ru.otus.hw.web.dto.DocumentDownload;
import ru.otus.hw.web.dto.DocumentDto;
import ru.otus.hw.web.dto.DocumentPageDto;
import ru.otus.hw.web.dto.DocumentUpload;

@RequiredArgsConstructor
@Service
public class DocumentServiceImpl implements DocumentService {

  private final DocumentClient client;

  public DocumentPageDto list(String token, UUID id, int page, int size) {
    return client.list(token, id, page, size);
  }

  public DocumentDto find(String token, UUID id) {
    return client.find(token, id);
  }

  public DocumentDto upload(String token, DocumentUpload input) {
    return client.upload(token, input);
  }

  public DocumentDownload download(String token, UUID id) {
    return client.download(token, id);
  }

  public void delete(String token, UUID id) {
    client.delete(token, id);
  }
}
