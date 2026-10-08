package ru.otus.hw.clinic.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.otus.hw.clinic.exception.InvalidDocumentException;

@Component
public class DocumentFileValidator {

  public static final int MAX_BYTES = 5 * 1024 * 1024;

  public static final long MAX_IMAGE_PIXELS = 20_000_000;

  private static final int MAX_FILENAME_LENGTH = 200;

  private static final int PDF_HEADER_LENGTH = 9;

  private static final int PDF_TAIL_LENGTH = 1024;

  private static final String PDF_MEDIA_TYPE = "application/pdf";

  private static final String PNG_MEDIA_TYPE = "image/png";

  private static final String JPEG_MEDIA_TYPE = "image/jpeg";

  private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

  private static final Pattern PDF_HEADER = Pattern.compile(
    "%PDF-(1\\.[0-7]|2\\.0)[\\r\\n ].*");

  public ValidatedDocument validate(MultipartFile file) {
    String name = validateFilename(file.getOriginalFilename());
    String type = resolveMediaType(name);
    validateDeclaredMediaType(type, file.getContentType());
    byte[] bytes = readBytes(file);
    validateSignature(type, bytes);
    if (!PDF_MEDIA_TYPE.equals(type)) {
      validateImage(bytes);
    }
    return new ValidatedDocument(name, type, bytes, DocumentChecksums.sha256(bytes));
  }

  private static String validateFilename(String originalName) {
    if (originalName == null || originalName.isBlank()) {
      throw badRequest("A filename is required");
    }
    String name = Normalizer.normalize(originalName, Normalizer.Form.NFC);
    if (name.length() > MAX_FILENAME_LENGTH || containsPathSyntax(name)
      || name.codePoints().anyMatch(Character::isISOControl)) {
      throw badRequest("Invalid filename");
    }
    return name;
  }

  private static boolean containsPathSyntax(String name) {
    return name.indexOf(':') >= 0 || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0;
  }

  private static String resolveMediaType(String name) {
    int dot = name.lastIndexOf('.');
    String extension = dot > 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    return switch (extension) {
      case "pdf" -> PDF_MEDIA_TYPE;
      case "png" -> PNG_MEDIA_TYPE;
      case "jpg", "jpeg" -> JPEG_MEDIA_TYPE;
      default -> throw unsupportedType("Use PDF, JPEG or PNG");
    };
  }

  private static void validateDeclaredMediaType(String expected, String declared) {
    if (!expected.equalsIgnoreCase(declared)) {
      throw unsupportedType("File extension and declared media type must match");
    }
  }

  private static byte[] readBytes(MultipartFile file) {
    if (file.getSize() > MAX_BYTES) {
      throw tooLarge();
    }
    byte[] bytes;
    try (var input = file.getInputStream()) {
      bytes = input.readNBytes(MAX_BYTES + 1);
    } catch (IOException exception) {
      throw badRequest("Could not read uploaded file");
    }
    validateSize(bytes.length);
    return bytes;
  }

  private static void validateSize(int size) {
    if (size == 0) {
      throw badRequest("Empty files are not accepted");
    }
    if (size > MAX_BYTES) {
      throw tooLarge();
    }
  }

  private static void validateSignature(String type, byte[] bytes) {
    boolean valid = switch (type) {
      case PDF_MEDIA_TYPE -> hasPdfSignature(bytes);
      case PNG_MEDIA_TYPE -> hasPngSignature(bytes);
      default -> hasJpegSignature(bytes);
    };
    if (!valid) {
      throw badRequest("File content does not match its declared type");
    }
  }

  private static boolean hasPdfSignature(byte[] bytes) {
    String header = new String(bytes, 0, Math.min(bytes.length, PDF_HEADER_LENGTH),
      StandardCharsets.ISO_8859_1);
    String tail = new String(bytes, Math.max(0, bytes.length - PDF_TAIL_LENGTH),
      Math.min(bytes.length, PDF_TAIL_LENGTH), StandardCharsets.ISO_8859_1);
    return PDF_HEADER.matcher(header).matches() && tail.stripTrailing().endsWith("%%EOF");
  }

  private static boolean hasPngSignature(byte[] bytes) {
    return bytes.length >= PNG_SIGNATURE.length
      && Arrays.equals(Arrays.copyOf(bytes, PNG_SIGNATURE.length), PNG_SIGNATURE);
  }

  private static boolean hasJpegSignature(byte[] bytes) {
    if (bytes.length < 4) {
      return false;
    }
    return hasJpegHeader(bytes) && hasJpegEnd(bytes);
  }

  private static boolean hasJpegHeader(byte[] bytes) {
    return unsigned(bytes[0]) == 255 && unsigned(bytes[1]) == 216 && unsigned(bytes[2]) == 255;
  }

  private static boolean hasJpegEnd(byte[] bytes) {
    return unsigned(bytes[bytes.length - 2]) == 255 && unsigned(bytes[bytes.length - 1]) == 217;
  }

  private static int unsigned(byte value) {
    return value & 255;
  }

  private static void validateImage(byte[] bytes) {
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      validateImageInput(input);
    } catch (InvalidDocumentException exception) {
      throw exception;
    } catch (IOException | RuntimeException exception) {
      throw badRequest("Invalid or truncated image");
    }
  }

  private static void validateImageInput(ImageInputStream input) throws IOException {
    var readers = ImageIO.getImageReaders(input);
    if (!readers.hasNext()) {
      throw badRequest("Invalid image");
    }
    var reader = readers.next();
    try {
      reader.setInput(input, true, true);
      validateImageDimensions(reader);
      if (reader.read(0) == null) {
        throw badRequest("Invalid image");
      }
    } finally {
      reader.dispose();
    }
  }

  private static void validateImageDimensions(ImageReader reader) throws IOException {
    int width = reader.getWidth(0);
    int height = reader.getHeight(0);
    long pixels = (long) width * height;
    if (width <= 0 || height <= 0 || pixels > MAX_IMAGE_PIXELS) {
      throw badRequest("Image exceeds 20 million pixels");
    }
  }

  private static InvalidDocumentException badRequest(String message) {
    return new InvalidDocumentException(HttpStatus.BAD_REQUEST, message);
  }

  private static InvalidDocumentException unsupportedType(String message) {
    return new InvalidDocumentException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
  }

  private static InvalidDocumentException tooLarge() {
    return new InvalidDocumentException(HttpStatus.PAYLOAD_TOO_LARGE,
      "File limit is 5 MiB (5242880 bytes)");
  }
}
