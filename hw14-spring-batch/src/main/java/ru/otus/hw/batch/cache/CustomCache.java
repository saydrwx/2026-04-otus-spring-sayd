package ru.otus.hw.batch.cache;

public interface CustomCache<T> {

  T get(Long key);

  void put(Long key, T value);

  void clear();
}
