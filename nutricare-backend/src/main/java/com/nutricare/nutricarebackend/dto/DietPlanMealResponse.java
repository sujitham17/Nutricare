package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.MealType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DietPlanMealResponse {

    private Long id;
    private Integer dayNumber;
    private LocalDate date;
    private MealType mealType;
    private String mealName;
    private String mealTime;
    private String waterIntake;
    private String instructions;
}
