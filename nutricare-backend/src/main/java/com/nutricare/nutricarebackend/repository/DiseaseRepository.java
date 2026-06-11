package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.Disease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Long> {
    Optional<Disease> findByNameIgnoreCase(String name);
    List<Disease> findByStatusIgnoreCase(String status);
}
