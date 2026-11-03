package ru.otus.hw.clinic.storage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentFileStore {

  void write(UUID key, byte[] bytes);

  byte[] read(UUID key, long expectedSize, String expectedSha256);

  void delete(UUID key);

  List<UUID> oldFileKeys(Instant cutoff);
}
