package ru.otus.hw.repositories.custom;

public interface CustomizedBookSave<T> {

  <S extends T> S save(S entity);
}
