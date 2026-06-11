package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ReportSummaryResponse {

    private Long userId;
    private String userFullName;
    private String userEmail;
    private HealthTrackingResponse latestHealth;
    private UserSubscriptionResponse subscription;
    private List<HealthTrackingResponse> healthRecords;
    private List<AppointmentResponse> appointments;
    private List<DietPlanResponse> dietPlans;
    private List<MealComplianceResponse> mealCompliance;
    private List<MealComplianceSummaryResponse> mealComplianceSummary;
    private List<ConsultationPaymentResponse> payments;
    private BigDecimal totalPaid;
    private Integer overallCompliancePercent;
    private Map<String, Long> appointmentCounts;
}
