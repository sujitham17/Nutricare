package com.nutricare.nutricarebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietPlanResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long dieticianId;
    private String dieticianFullName;
    private String dieticianEmail;
    private String title;
    private String description;
    private String programGoal;
    private String breakfast;
    private String lunch;
    private String dinner;
    private String snacks;
    private String waterIntake;
    private Integer calories;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private List<DietPlanMealResponse> meals;
}
