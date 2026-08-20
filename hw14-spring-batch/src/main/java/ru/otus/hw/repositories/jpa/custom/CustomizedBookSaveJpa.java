package ru.otus.hw.repositories.jpa.custom;

public interface CustomizedBookSaveJpa<T> {

  <S extends T> S save(S entity);
}
