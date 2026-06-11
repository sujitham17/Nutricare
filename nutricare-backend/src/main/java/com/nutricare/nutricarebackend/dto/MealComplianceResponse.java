package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.MealComplianceStatus;
import com.nutricare.nutricarebackend.entity.MealType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MealComplianceResponse {

    private Long id;
    private Long mealId;
    private Long dietPlanId;
    private String dietPlanTitle;
    private Integer dayNumber;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long dieticianId;
    private String dieticianFullName;
    private MealType mealType;
    private String mealName;
    private String mealTime;
    private MealComplianceStatus status;
    private String reason;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
