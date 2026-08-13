package ru.otus.hw.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.hw.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(String name);
}
