package ru.otus.hw.batch.cache;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import ru.otus.hw.models.mongo.AuthorMongo;

@Component
public class AuthorCustomCache implements CustomCache<AuthorMongo> {

  private final ConcurrentHashMap<Long, AuthorMongo> cache = new ConcurrentHashMap<>();

  public AuthorMongo get(Long key) {
    return cache.get(key);
  }

  public void put(Long key, AuthorMongo value) {
    cache.putIfAbsent(key, value);
  }

  public void clear() {
    cache.clear();
  }
}
