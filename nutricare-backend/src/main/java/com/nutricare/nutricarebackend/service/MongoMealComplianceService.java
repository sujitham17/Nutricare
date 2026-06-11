package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.DietPlanMeal;
import com.nutricare.nutricarebackend.entity.MealCompliance;
import com.nutricare.nutricarebackend.mongo.document.MealComplianceLogDocument;
import com.nutricare.nutricarebackend.mongo.repository.MealComplianceLogMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MongoMealComplianceService {

    private final MealComplianceLogMongoRepository mealComplianceLogMongoRepository;

    public MealComplianceLogDocument saveSubmission(MealCompliance compliance) {
        DietPlanMeal meal = compliance.getDietPlanMeal();
        DietPlan dietPlan = meal.getDietPlan();
        LocalDate mealDate = dietPlan.getStartDate() == null || meal.getDayNumber() == null
                ? null
                : dietPlan.getStartDate().plusDays(meal.getDayNumber() - 1L);

        return mealComplianceLogMongoRepository.save(MealComplianceLogDocument.builder()
                .mysqlComplianceId(compliance.getId())
                .userId(compliance.getUser().getId())
                .dieticianId(dietPlan.getDietician().getId())
                .weeklyMealPlanId(dietPlan.getMongoDocumentId())
                .mealDate(mealDate)
                .mealType(compliance.getMealType())
                .status(compliance.getStatus())
                .reason(compliance.getReason())
                .submittedAt(compliance.getSubmittedAt())
                .build());
    }
}
