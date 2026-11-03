package ru.otus.hw.directory.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.otus.hw.directory.model.Hospital;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

  List<Hospital> findAllByOrderByNameAsc();
}
