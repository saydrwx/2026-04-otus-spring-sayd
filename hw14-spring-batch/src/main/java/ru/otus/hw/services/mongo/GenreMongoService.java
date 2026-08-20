package ru.otus.hw.services.mongo;

import java.util.Set;
import ru.otus.hw.models.mongo.GenreMongo;

import java.util.List;

public interface GenreMongoService {

  List<GenreMongo> findAll();

  List<GenreMongo> findAllByIds(Set<String> ids);
}
