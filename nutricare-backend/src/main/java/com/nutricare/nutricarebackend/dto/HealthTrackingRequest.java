package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class HealthTrackingRequest {

    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;
    private String bloodPressure;
    private BigDecimal sugarLevel;
    private BigDecimal waterIntake;
    private BigDecimal sleepHours;
    private String activityLevel;
    private String notes;

    @NotNull
    private LocalDate recordedDate;
}
