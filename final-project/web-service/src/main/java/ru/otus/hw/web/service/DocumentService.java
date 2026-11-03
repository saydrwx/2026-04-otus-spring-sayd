package ru.otus.hw.web.service;

import java.util.UUID;
import ru.otus.hw.web.dto.DocumentDownload;
import ru.otus.hw.web.dto.DocumentDto;
import ru.otus.hw.web.dto.DocumentPageDto;
import ru.otus.hw.web.dto.DocumentUpload;

public interface DocumentService {

  DocumentPageDto list(String token, UUID patientId, int page, int size);

  DocumentDto find(String token, UUID id);

  DocumentDto upload(String token, DocumentUpload input);

  DocumentDownload download(String token, UUID id);

  void delete(String token, UUID id);
}
