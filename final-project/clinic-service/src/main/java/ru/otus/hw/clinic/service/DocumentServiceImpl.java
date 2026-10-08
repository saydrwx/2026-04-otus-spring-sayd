package ru.otus.hw.clinic.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.otus.hw.clinic.dto.DocumentDto;
import ru.otus.hw.clinic.dto.DocumentPageDto;
import ru.otus.hw.clinic.security.CurrentAccount;
import ru.otus.hw.clinic.security.DoctorIdentity;
import ru.otus.hw.clinic.storage.DocumentDescriptor;
import ru.otus.hw.clinic.storage.DocumentDownload;
import ru.otus.hw.clinic.storage.DocumentFileStore;
import ru.otus.hw.clinic.storage.DocumentFileValidator;
import ru.otus.hw.clinic.storage.ValidatedDocument;

@RequiredArgsConstructor
@Service
public class DocumentServiceImpl implements DocumentService {

  private final DocumentFileValidator validator;

  private final DocumentFileStore files;

  private final DocumentMetadataService metadata;

  private final DocumentCleanupService cleanup;

  private final CurrentAccount account;

  private final DoctorIdentityService doctorIdentityService;

  @Override
  @PreAuthorize("hasRole('PATIENT')")
  public DocumentDto upload(MultipartFile file) {
    ValidatedDocument validated = validator.validate(file);
    UUID id = metadata.reserve(validated);
    try {
      files.write(id, validated.bytes());
      return metadata.activate(id);
    } catch (RuntimeException exception) {
      cleanup.failedUpload(id);
      throw exception;
    }
  }

  @Override
  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
  public DocumentDto find(UUID id) {
    return metadata.findReady(id, doctor()).metadata();
  }

  @Override
  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
  public DocumentPageDto list(UUID patientId, int page, int size) {
    return metadata.list(patientId, page, size, doctor());
  }

  @Override
  @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
  public DocumentDownload download(UUID id) {
    DocumentDescriptor authorized = metadata.findReady(id, doctor());
    byte[] bytes = files.read(id, authorized.metadata().sizeBytes(), authorized.sha256());
    return new DocumentDownload(authorized.metadata(), bytes);
  }

  @Override
  @PreAuthorize("hasRole('PATIENT')")
  public void delete(UUID id) {
    metadata.requestDelete(id);
    cleanup.tryDelete(id);
  }

  private DoctorIdentity doctor() {
    return account.hasRole("DOCTOR") ? doctorIdentityService.current() : null;
  }
}
