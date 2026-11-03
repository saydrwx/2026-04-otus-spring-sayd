package ru.otus.hw.web.client;

import java.util.UUID;
import ru.otus.hw.web.dto.*;

public interface DocumentClient {

  DocumentPageDto list(String token, UUID patientId, int page, int size);

  DocumentDto find(String token, UUID id);

  DocumentDto upload(String token, DocumentUpload input);

  DocumentDownload download(String token, UUID id);

  void delete(String token, UUID id);
}
