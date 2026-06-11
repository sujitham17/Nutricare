package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DietPlanRequest {

    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @Size(max = 255)
    private String programGoal;

    @Size(max = 1000)
    private String breakfast;

    @Size(max = 1000)
    private String lunch;

    @Size(max = 1000)
    private String dinner;

    @Size(max = 1000)
    private String snacks;

    @Size(max = 255)
    private String waterIntake;

    @PositiveOrZero
    private Integer calories;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private List<WeeklyMealRequest> meals;
}
