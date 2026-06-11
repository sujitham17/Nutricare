package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.DietPlanMeal;
import com.nutricare.nutricarebackend.entity.MealType;
import com.nutricare.nutricarebackend.mongo.document.WeeklyMealPlanDocument;
import com.nutricare.nutricarebackend.mongo.repository.WeeklyMealPlanMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MongoWeeklyMealPlanService {

    private final WeeklyMealPlanMongoRepository weeklyMealPlanMongoRepository;

    public WeeklyMealPlanDocument saveWeeklyPlan(DietPlan dietPlan, List<DietPlanMeal> meals) {
        LocalDateTime now = LocalDateTime.now();
        WeeklyMealPlanDocument document = weeklyMealPlanMongoRepository.findByDietPlanId(dietPlan.getId())
                .orElseGet(WeeklyMealPlanDocument::new);

        document.setDietPlanId(dietPlan.getId());
        document.setMysqlUserId(dietPlan.getUser().getId());
        document.setMysqlDieticianId(dietPlan.getDietician().getId());
        document.setTitle(dietPlan.getTitle());
        document.setDescription(dietPlan.getDescription());
        document.setProgramGoal(dietPlan.getProgramGoal());
        document.setWaterIntake(dietPlan.getWaterIntake());
        document.setCalories(dietPlan.getCalories());
        document.setStartDate(dietPlan.getStartDate());
        document.setEndDate(dietPlan.getEndDate());
        document.setDays(buildDays(dietPlan, meals));
        document.setMealDetails(meals.stream()
                .map(meal -> WeeklyMealPlanDocument.WeeklyMealDetail.builder()
                        .mysqlMealId(meal.getId())
                        .dayNumber(meal.getDayNumber())
                        .mealType(meal.getMealType())
                        .mealName(meal.getMealName())
                        .mealTime(meal.getMealTime())
                        .waterIntake(meal.getWaterIntake())
                        .instructions(meal.getInstructions())
                        .build())
                .toList());
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(now);
        }
        document.setUpdatedAt(now);
        return weeklyMealPlanMongoRepository.save(document);
    }

    public Optional<WeeklyMealPlanDocument> findByDietPlanId(Long dietPlanId) {
        return weeklyMealPlanMongoRepository.findByDietPlanId(dietPlanId);
    }

    private List<WeeklyMealPlanDocument.WeeklyMealDay> buildDays(DietPlan dietPlan, List<DietPlanMeal> meals) {
        return java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(day -> {
                    List<DietPlanMeal> dayMeals = meals.stream()
                            .filter(meal -> meal.getDayNumber() != null && meal.getDayNumber() == day)
                            .toList();
                    LocalDate date = dietPlan.getStartDate() == null ? null : dietPlan.getStartDate().plusDays(day - 1L);
                    return WeeklyMealPlanDocument.WeeklyMealDay.builder()
                            .date(date)
                            .breakfast(mealName(dayMeals, MealType.BREAKFAST))
                            .lunch(mealName(dayMeals, MealType.LUNCH))
                            .dinner(mealName(dayMeals, MealType.DINNER))
                            .snacks(mealName(dayMeals, MealType.SNACK))
                            .instructions(dayMeals.stream()
                                    .map(DietPlanMeal::getInstructions)
                                    .filter(value -> value != null && !value.isBlank())
                                    .collect(Collectors.joining("\n")))
                            .build();
                })
                .toList();
    }

    private String mealName(List<DietPlanMeal> meals, MealType mealType) {
        return meals.stream()
                .filter(meal -> meal.getMealType() == mealType)
                .map(DietPlanMeal::getMealName)
                .findFirst()
                .orElse(null);
    }
}
