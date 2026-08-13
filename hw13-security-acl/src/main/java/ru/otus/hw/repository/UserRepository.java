package ru.otus.hw.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

  @EntityGraph(attributePaths = {"roles"})
  Optional<User> findByName(String name);

  boolean existsByName(String name);
}
