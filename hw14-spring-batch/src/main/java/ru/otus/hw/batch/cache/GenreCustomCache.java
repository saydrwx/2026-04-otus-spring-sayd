package ru.otus.hw.batch.cache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import ru.otus.hw.models.mongo.GenreMongo;

@Component
public class GenreCustomCache implements CustomCache<GenreMongo> {

  private final ConcurrentHashMap<Long, GenreMongo> cache = new ConcurrentHashMap<>();

  public GenreMongo get(Long key) {
    return cache.get(key);
  }

  public List<GenreMongo> get(List<Long> keys) {
    return keys.stream().map(cache::get).toList();
  }

  public void put(Long key, GenreMongo value) {
    cache.putIfAbsent(key, value);
  }

  public void clear() {
    cache.clear();
  }
}
