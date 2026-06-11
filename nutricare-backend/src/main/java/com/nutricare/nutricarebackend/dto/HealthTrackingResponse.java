package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class HealthTrackingResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;
    private String bloodPressure;
    private BigDecimal sugarLevel;
    private BigDecimal waterIntake;
    private BigDecimal sleepHours;
    private String activityLevel;
    private String notes;
    private LocalDate recordedDate;
    private LocalDateTime createdAt;
    private Integer age;
    private String gender;
    private String goal;
}
