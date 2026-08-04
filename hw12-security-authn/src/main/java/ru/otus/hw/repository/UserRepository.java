package ru.otus.hw.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByName(String name);

  boolean existsByName(String name);
}
