package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.HealthTracking;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthTrackingRepository extends JpaRepository<HealthTracking, Long> {

    List<HealthTracking> findByUserOrderByRecordedDateDescCreatedAtDesc(User user);

    Optional<HealthTracking> findFirstByUserOrderByRecordedDateDescCreatedAtDesc(User user);

    Optional<HealthTracking> findByIdAndUser(Long id, User user);
}
