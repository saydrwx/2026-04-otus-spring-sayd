package ru.otus.hw.batch.cache;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import ru.otus.hw.models.mongo.BookMongo;

@Component
public class BookCustomCache implements CustomCache<BookMongo> {

  private final ConcurrentHashMap<Long, BookMongo> cache = new ConcurrentHashMap<>();

  public BookMongo get(Long key) {
    return cache.get(key);
  }

  public void put(Long key, BookMongo value) {
    cache.putIfAbsent(key, value);
  }

  public void clear() {
    cache.clear();
  }
}
