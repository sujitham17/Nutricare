package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class UserHealthProfileResponse {

    private Long userId;
    private String fullName;
    private String email;
    private Integer age;
    private String gender;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bmi;
    private String goal;
    private String activityLevel;
    private String allergies;
    private String medicalConditions;
    private String foodPreference;
    private String specialCondition;
    private String notes;
    private LocalDate latestRecordedDate;
}
