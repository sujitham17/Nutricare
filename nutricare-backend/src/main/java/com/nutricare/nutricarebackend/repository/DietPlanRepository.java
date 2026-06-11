package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {

    List<DietPlan> findByUserOrderByCreatedAtDesc(User user);

    List<DietPlan> findByDieticianOrderByCreatedAtDesc(User dietician);

    List<DietPlan> findByUserAndDieticianOrderByCreatedAtDesc(User user, User dietician);
}
