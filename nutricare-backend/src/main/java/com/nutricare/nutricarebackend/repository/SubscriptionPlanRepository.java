package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.SubscriptionPlan;
import com.nutricare.nutricarebackend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findByActiveTrueOrderByPriceAsc();

    List<SubscriptionPlan> findByPlanAudienceAndActiveTrueOrderByPriceAsc(Role planAudience);

    Optional<SubscriptionPlan> findByPlanNameAndPlanAudience(String planName, Role planAudience);

    boolean existsByPlanNameAndPlanAudience(String planName, Role planAudience);
}
