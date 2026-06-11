package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MealComplianceSummaryResponse {

    private String mealType;
    private long followed;
    private long total;
    private long pending;
    private long notFollowed;
    private int compliancePercent;
}
