package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.DieticianProfile;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DieticianProfileRepository extends JpaRepository<DieticianProfile, Long> {

    Optional<DieticianProfile> findByUser(User user);

    boolean existsByUser(User user);
}
