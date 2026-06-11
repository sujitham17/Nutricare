package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.DietPlanMeal;
import com.nutricare.nutricarebackend.entity.MealType;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DietPlanMealRepository extends JpaRepository<DietPlanMeal, Long> {

    List<DietPlanMeal> findByDietPlanOrderByIdAsc(DietPlan dietPlan);

    List<DietPlanMeal> findByDietPlanOrderByDayNumberAscMealTypeAsc(DietPlan dietPlan);

    List<DietPlanMeal> findByDietPlanUserOrderByDietPlanCreatedAtDescIdAsc(User user);

    List<DietPlanMeal> findByDietPlanDieticianOrderByDietPlanCreatedAtDescIdAsc(User dietician);

    List<DietPlanMeal> findByDietPlanUserAndDietPlanDieticianOrderByDietPlanCreatedAtDescIdAsc(User user, User dietician);

    Optional<DietPlanMeal> findByDietPlanAndDayNumberAndMealType(DietPlan dietPlan, Integer dayNumber, MealType mealType);
}
