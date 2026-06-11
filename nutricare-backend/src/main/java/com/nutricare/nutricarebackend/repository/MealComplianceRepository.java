package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.DietPlanMeal;
import com.nutricare.nutricarebackend.entity.MealCompliance;
import com.nutricare.nutricarebackend.entity.MealComplianceStatus;
import com.nutricare.nutricarebackend.entity.MealType;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.DietPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealComplianceRepository extends JpaRepository<MealCompliance, Long> {

    Optional<MealCompliance> findByDietPlanMealAndUser(DietPlanMeal dietPlanMeal, User user);

    List<MealCompliance> findByUserOrderByCreatedAtDesc(User user);

    List<MealCompliance> findByDietPlanMealDietPlanDieticianOrderByCreatedAtDesc(User dietician);

    List<MealCompliance> findByDietPlanMealDietPlanDieticianAndStatusOrderByCreatedAtDesc(User dietician, MealComplianceStatus status);

    List<MealCompliance> findByUserAndDietPlanMealDietPlanDieticianOrderByCreatedAtDesc(User user, User dietician);

    long countByStatus(MealComplianceStatus status);

    long countByUserAndMealTypeAndStatus(User user, MealType mealType, MealComplianceStatus status);

    long countByUserAndMealType(User user, MealType mealType);

    long countByUserAndStatus(User user, MealComplianceStatus status);

    long countByUser(User user);

    long countByDietPlanMealDietPlan(DietPlan dietPlan);

    long countByDietPlanMealDietPlanAndStatus(DietPlan dietPlan, MealComplianceStatus status);
}
