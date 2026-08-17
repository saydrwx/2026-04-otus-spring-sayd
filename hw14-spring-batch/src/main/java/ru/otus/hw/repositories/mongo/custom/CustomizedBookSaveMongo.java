package ru.otus.hw.repositories.mongo.custom;

public interface CustomizedBookSaveMongo<T> {

  <S extends T> S save(S entity);
}
