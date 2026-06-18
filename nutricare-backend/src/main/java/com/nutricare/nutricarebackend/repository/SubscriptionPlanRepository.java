package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findByActiveTrueOrderByPriceAsc();

    List<SubscriptionPlan> findByRoleTypeAndActiveTrueOrderByPriceAsc(String roleType);

    Optional<SubscriptionPlan> findByNameAndRoleType(String name, String roleType);

    boolean existsByNameAndRoleType(String name, String roleType);
}
