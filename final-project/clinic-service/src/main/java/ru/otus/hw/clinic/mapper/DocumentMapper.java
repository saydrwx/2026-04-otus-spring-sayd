package ru.otus.hw.clinic.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.dto.DocumentDto;
import ru.otus.hw.clinic.model.PatientDocument;

@Component
public class DocumentMapper {

  public DocumentDto toDto(PatientDocument d) {
    return new DocumentDto(
      d.getId(),
      d.getPatient().getId(),
      d.getFilename(),
      d.getMediaType(),
      d.getSizeBytes(),
      d.getCreatedAt()
    );
  }
}
