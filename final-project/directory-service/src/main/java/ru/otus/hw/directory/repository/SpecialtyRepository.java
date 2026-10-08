package ru.otus.hw.directory.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.otus.hw.directory.model.Specialty;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

  List<Specialty> findAllByOrderByNameAsc();
}
