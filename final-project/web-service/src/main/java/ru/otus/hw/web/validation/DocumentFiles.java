package ru.otus.hw.web.validation;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.otus.hw.web.dto.DocumentDto;
import ru.otus.hw.web.dto.DocumentUpload;
import ru.otus.hw.web.exception.DocumentsException;

import static ru.otus.hw.web.exception.DocumentsException.Kind.*;

@Component("documentFiles")
public class DocumentFiles {

  public static final int MAX_BYTES = 5 * 1024 * 1024;

  public DocumentUpload prepare(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new DocumentsException(INVALID);
    }
    if (file.getSize() > MAX_BYTES) {
      throw new DocumentsException(TOO_LARGE);
    }
    String name = filename(file.getOriginalFilename());
    String expected = mediaType(name);
    String declared = file.getContentType();
    // Generic browser MIME declarations are normalized, but the backend still verifies actual content.
    if (declared != null && !declared.isBlank() && !declared.equalsIgnoreCase(
      "application/octet-stream") && !declared.equalsIgnoreCase(expected)) {
      throw new DocumentsException(TYPE);
    }
    try (var input = file.getInputStream()) {
      byte[] bytes = input.readNBytes(MAX_BYTES + 1);
      if (bytes.length > MAX_BYTES) {
        throw new DocumentsException(TOO_LARGE);
      }
      if (bytes.length == 0 || bytes.length != file.getSize()) {
        throw new DocumentsException(INVALID);
      }
      return new DocumentUpload(name, expected, bytes);
    } catch (IOException exception) {
      throw new DocumentsException(INVALID);
    }
  }

  public String filename(String value) {
    if (value == null || value.isBlank()) {
      throw new DocumentsException(INVALID);
    }
    String name = Normalizer.normalize(value, Normalizer.Form.NFC);
    if (name.length() > 200 || name.indexOf(':') >= 0 || name.indexOf('/') >= 0
      || name.indexOf('\\') >= 0
      || name.codePoints().anyMatch(Character::isISOControl)) {
      throw new DocumentsException(INVALID);
    }
    return name;
  }

  public String mediaType(String name) {
    int dot = name.lastIndexOf('.');
    String extension = dot > 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    return switch (extension) {
      case "pdf" -> "application/pdf";
      case "png" -> "image/png";
      case "jpg", "jpeg" -> "image/jpeg";
      default -> throw new DocumentsException(TYPE);
    };
  }

  public boolean valid(DocumentDto value) {
    if (value == null || value.id() == null || value.patientId() == null || value.sizeBytes() < 1
      || value.sizeBytes() > MAX_BYTES || value.uploadedAt() == null) {
      return false;
    }
    try {
      return filename(value.filename()).equals(value.filename()) && mediaType(
        value.filename()).equals(value.mediaType());
    } catch (DocumentsException exception) {
      return false;
    }
  }

  public String sizeLabel(long bytes) {
    if (bytes < 1024) {
      return bytes + " байт";
    }
    return bytes < 1024 * 1024 ? String.format(Locale.forLanguageTag("ru-RU"), "%.1f КиБ",
      bytes / 1024d)
      : String.format(Locale.forLanguageTag("ru-RU"), "%.1f МиБ", bytes / (1024d * 1024d));
  }

  public String typeLabel(String type) {
    return switch (type) {
      case "application/pdf" -> "PDF";
      case "image/png" -> "PNG";
      case "image/jpeg" -> "JPEG";
      default -> "Документ";
    };
  }
}
