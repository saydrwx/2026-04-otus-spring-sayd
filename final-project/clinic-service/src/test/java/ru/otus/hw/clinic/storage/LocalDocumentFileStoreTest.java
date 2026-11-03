package ru.otus.hw.clinic.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.otus.hw.clinic.configuration.DocumentProperties;
import ru.otus.hw.clinic.exception.DocumentStorageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDocumentFileStoreTest {

  @TempDir
  Path root;

  @Test
  void writesChecksumsAndDeletesGeneratedKeys() {
    var store = new LocalDocumentFileStore(new DocumentProperties(root, Duration.ofHours(1)));
    var key = UUID.randomUUID();
    byte[] content = {1, 2, 3};
    store.write(key, content);
    assertThat(store.read(key, 3, DocumentChecksums.sha256(content))).isEqualTo(content);
    assertThatThrownBy(() -> store.read(key, 4, DocumentChecksums.sha256(content))).isInstanceOf(
      DocumentStorageException.class);
    store.delete(key);
    store.delete(key);
    assertThatThrownBy(() -> store.read(key, 3, DocumentChecksums.sha256(content))).isInstanceOf(
      DocumentStorageException.class);
  }

  @Test
  void doesNotFollowAnObjectSymlink() throws Exception {
    var store = new LocalDocumentFileStore(new DocumentProperties(root, Duration.ofHours(1)));
    Path outside = root.resolve("private.txt");
    Files.writeString(outside, "private");
    UUID key = UUID.randomUUID();
    try {
      Files.createSymbolicLink(root.resolve("objects").resolve(key + ".bin"), outside);
    } catch (UnsupportedOperationException | java.nio.file.FileSystemException exception) {
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symlinks unavailable on this platform");
    }
    assertThatThrownBy(() -> store.read(key, 7,
      DocumentChecksums.sha256("private".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
      .isInstanceOf(DocumentStorageException.class);
    store.delete(key);
    assertThat(Files.readString(outside)).isEqualTo("private");
  }
}
