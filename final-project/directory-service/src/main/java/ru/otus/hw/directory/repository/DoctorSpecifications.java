package ru.otus.hw.directory.repository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import ru.otus.hw.directory.model.Doctor;

public final class DoctorSpecifications {

  private DoctorSpecifications() {

  }

  public static Specification<Doctor> matching(Long hospitalId, List<Long> specialtyIds) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (hospitalId != null) {
        predicates.add(
          cb.equal(root.get("hospital").get("id"), hospitalId)
        );
      }

      if (specialtyIds != null && !specialtyIds.isEmpty()) {
        predicates.add(
          root.join("specialties").get("id").in(specialtyIds)
        );

        if (query != null) {
          query.distinct(true);
        }
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
