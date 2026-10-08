package ru.otus.hw.clinic.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.otus.hw.clinic.configuration.DocumentProperties;
import ru.otus.hw.clinic.exception.DocumentStorageException;

@Component
public class LocalDocumentFileStore implements DocumentFileStore {

  private final Path staging;

  private final Path objects;

  public LocalDocumentFileStore(DocumentProperties properties) {
    Path root = properties.root().toAbsolutePath().normalize();
    staging = root.resolve("staging");
    objects = root.resolve("objects");
    initializeDirectories(root);
  }

  @Override
  public void write(UUID key, byte[] bytes) {
    Path source = staging.resolve(key + ".part");
    Path target = objects.resolve(key + ".bin");
    try {
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Storage key already exists");
      }
      writeStagedFile(source, bytes);
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException | UnsupportedOperationException exception) {
      throw new DocumentStorageException(exception);
    }
  }

  @Override
  public byte[] read(UUID key, long expectedSize, String expectedSha256) {
    Path path = objects.resolve(key + ".bin");
    try {
      if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Document bytes missing");
      }
      byte[] bytes = readObjectBytes(path);
      verifyObject(bytes, expectedSize, expectedSha256);
      return bytes;
    } catch (IOException | UnsupportedOperationException exception) {
      throw new DocumentStorageException(exception);
    }
  }

  @Override
  public void delete(UUID key) {
    try {
      Files.deleteIfExists(objects.resolve(key + ".bin"));
      Files.deleteIfExists(staging.resolve(key + ".part"));
    } catch (IOException | UnsupportedOperationException exception) {
      throw new DocumentStorageException(exception);
    }
  }

  @Override
  public List<UUID> oldFileKeys(Instant cutoff) {
    Set<UUID> keys = new LinkedHashSet<>();
    try {
      collectOldKeys(objects, ".bin", cutoff, keys);
      collectOldKeys(staging, ".part", cutoff, keys);
      return List.copyOf(keys);
    } catch (IOException | UnsupportedOperationException exception) {
      throw new DocumentStorageException(exception);
    }
  }

  private void initializeDirectories(Path root) {
    try {
      for (Path directory : List.of(root, staging, objects)) {
        createPrivateDirectory(directory);
      }
    } catch (IOException | UnsupportedOperationException exception) {
      throw new DocumentStorageException(exception);
    }
  }

  private static void createPrivateDirectory(Path directory) throws IOException {
    Files.createDirectories(directory);
    if (Files.isSymbolicLink(directory)
      || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Document directories must be private real directories");
    }
    permissions(directory, "rwx------");
  }

  private static void writeStagedFile(Path source, byte[] bytes) throws IOException {
    try (var channel = FileChannel.open(
      source, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      permissions(source, "rw-------");
      var buffer = ByteBuffer.wrap(bytes);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
  }

  private static byte[] readObjectBytes(Path path) throws IOException {
    try (var input = Files.newInputStream(
      path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
      return input.readNBytes(DocumentFileValidator.MAX_BYTES + 1);
    }
  }

  private static void verifyObject(byte[] bytes, long expectedSize, String expectedSha256)
    throws IOException {
    if (bytes.length != expectedSize
      || !DocumentChecksums.sha256(bytes).equals(expectedSha256)) {
      throw new IOException("Document checksum/size mismatch");
    }
  }

  private static void collectOldKeys(Path directory, String suffix, Instant cutoff, Set<UUID> keys)
    throws IOException {
    try (var files = Files.list(directory)) {
      for (Path file : files.toList()) {
        addOldKey(file, suffix, cutoff, keys);
      }
    }
  }

  private static void addOldKey(Path file, String suffix, Instant cutoff, Set<UUID> keys)
    throws IOException {
    if (!isOldRegularFile(file, cutoff)) {
      return;
    }
    parseCanonicalKey(file.getFileName().toString(), suffix).ifPresent(keys::add);
  }

  private static boolean isOldRegularFile(Path file, Instant cutoff) throws IOException {
    return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
      && Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(cutoff);
  }

  private static Optional<UUID> parseCanonicalKey(String name, String suffix) {
    if (!name.endsWith(suffix)) {
      return Optional.empty();
    }
    String value = name.substring(0, name.length() - suffix.length());
    try {
      UUID key = UUID.fromString(value);
      if (!key.toString().equals(value)) {
        return Optional.empty();
      }
      return Optional.of(key);
    } catch (IllegalArgumentException exception) {
      // Unrelated or malformed operator files must not become cleanup candidates.
      return Optional.empty();
    }
  }

  private static void permissions(Path path, String mode) throws IOException {
    if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
    }
  }
}
