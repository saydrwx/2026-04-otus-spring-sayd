package ru.otus.hw.clinic.service;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import ru.otus.hw.clinic.dto.DocumentDto;
import ru.otus.hw.clinic.dto.DocumentPageDto;
import ru.otus.hw.clinic.storage.DocumentDownload;

public interface DocumentService {

  DocumentDto upload(MultipartFile file);

  DocumentDto find(UUID id);

  DocumentPageDto list(UUID patientId, int page, int size);

  DocumentDownload download(UUID id);

  void delete(UUID id);
}
