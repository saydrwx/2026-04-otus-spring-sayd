package ru.otus.hw.clinic.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import ru.otus.hw.clinic.exception.InvalidDocumentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFileValidatorTest {

  private final DocumentFileValidator validator = new DocumentFileValidator();
  private static final byte[] PDF = "%PDF-1.7\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

  @Test
  void acceptsPdfAndDecodablePngAndJpeg() throws Exception {
    assertThat(
      validator.validate(file("report.pdf", "application/pdf", PDF)).mediaType()).isEqualTo(
      "application/pdf");
    for (String format : new String[]{"png", "jpeg"}) {
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, bytes);
      assertThat(
        validator.validate(file("image." + format, "image/" + format, bytes.toByteArray())).bytes())
        .isEqualTo(bytes.toByteArray());
    }
  }

  @Test
  void rejectsEmptySpoofedAndTruncatedFiles() {
    for (byte[] bytes : new byte[][]{new byte[0], "text".getBytes(StandardCharsets.US_ASCII),
      "%PDF-1.7\nmissing end".getBytes(StandardCharsets.US_ASCII)}) {
      assertThatThrownBy(() -> validator.validate(file("x.pdf", "application/pdf", bytes)))
        .isInstanceOfSatisfying(InvalidDocumentException.class,
          e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
    assertThatThrownBy(() -> validator.validate(
      file("x.png", "image/png", new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10})))
      .isInstanceOf(InvalidDocumentException.class);
  }

  @Test
  void requiresMatchingExtensionMimeAndSafeFilename() {
    for (String name : new String[]{"../x.pdf", "C:\\x.pdf", "x\r\n.pdf", "x:y.pdf"}) {
      assertThatThrownBy(() -> validator.validate(file(name, "application/pdf", PDF))).isInstanceOf(
        InvalidDocumentException.class);
    }
    assertThatThrownBy(() -> validator.validate(file("x.exe", "application/pdf", PDF)))
      .isInstanceOfSatisfying(InvalidDocumentException.class,
        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    assertThatThrownBy(() -> validator.validate(file("x.pdf", "image/png", PDF)))
      .isInstanceOf(InvalidDocumentException.class);
  }

  @Test
  void acceptsExact5MiBAndRejectsOneByteMoreEvenWhenSizeIsMisreported() {
    byte[] exact = new byte[DocumentFileValidator.MAX_BYTES];
    Arrays.fill(exact, (byte) ' ');
    System.arraycopy(PDF, 0, exact, 0, 9);
    byte[] end = "%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(end, 0, exact, exact.length - end.length, end.length);
    assertThat(validator.validate(file("x.pdf", "application/pdf", exact)).bytes()).hasSize(
      exact.length);
    byte[] oversized = Arrays.copyOf(exact, exact.length + 1);
    var dishonest = new MockMultipartFile("file", "x.pdf", "application/pdf", oversized) {
      @Override
      public long getSize() {
        return 1;
      }
    };
    assertThatThrownBy(() -> validator.validate(dishonest))
      .isInstanceOfSatisfying(InvalidDocumentException.class,
        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
  }

  @Test
  void rejectsHugeImageDimensionsBeforeRasterDecoding() throws Exception {
    var bytes = new ByteArrayOutputStream();
    var output = new java.io.DataOutputStream(bytes);
    output.write(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
    byte[] header = java.nio.ByteBuffer.allocate(17).put(new byte[]{73, 72, 68, 82})
      .putInt(50000).putInt(50000).put(new byte[]{8, 2, 0, 0, 0}).array();
    var crc = new java.util.zip.CRC32();
    crc.update(header);
    output.writeInt(13);
    output.write(header);
    output.writeInt((int) crc.getValue());
    output.write(new byte[]{0, 0, 0, 0, 73, 69, 78, 68, (byte) 174, 66, 96, (byte) 130});
    assertThatThrownBy(() -> validator.validate(file("huge.png", "image/png", bytes.toByteArray())))
      .isInstanceOf(InvalidDocumentException.class);
  }

  private MockMultipartFile file(String name, String type, byte[] bytes) {
    return new MockMultipartFile("file", name, type, bytes);
  }
}
