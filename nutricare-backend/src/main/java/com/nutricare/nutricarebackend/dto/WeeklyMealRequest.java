package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeeklyMealRequest {

    @NotNull
    @Min(1)
    @Max(7)
    private Integer dayNumber;

    @NotNull
    private MealType mealType;

    @NotBlank
    @Size(max = 1000)
    private String mealName;

    @Size(max = 100)
    private String mealTime;

    @Size(max = 255)
    private String waterIntake;

    @Size(max = 1000)
    private String instructions;
}
