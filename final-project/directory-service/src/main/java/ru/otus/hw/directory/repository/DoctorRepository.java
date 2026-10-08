package ru.otus.hw.directory.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.otus.hw.directory.model.Doctor;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long>,
  JpaSpecificationExecutor<Doctor> {

  @Override
  @EntityGraph(attributePaths = {"hospital", "specialties"})
  Optional<Doctor> findById(Long id);

  @Override
  @EntityGraph(attributePaths = {"hospital", "specialties"})
  List<Doctor> findAll(Specification<Doctor> specification, Sort sort);
}
