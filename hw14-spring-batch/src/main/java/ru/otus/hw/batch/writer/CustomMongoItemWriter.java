package ru.otus.hw.batch.writer;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.otus.hw.batch.cache.CustomCache;
import ru.otus.hw.models.jpa.JpaEntity;

@RequiredArgsConstructor
public class CustomMongoItemWriter<T extends JpaEntity, V> implements ItemWriter<Pair<T, V>> {

  private final MongoTemplate mongoTemplate;

  private final CustomCache<V> cache;

  @Override
  public void write(Chunk<? extends Pair<T, V>> chunk) {
    chunk.getItems().forEach(item -> {
      V val = item.getValue();
      mongoTemplate.insert(val);
      cache.put(item.getKey().getId(), val);
    });
  }
}
