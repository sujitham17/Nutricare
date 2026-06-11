package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AdminDashboardSummaryResponse {

    private long totalUsers;
    private long totalDieticians;
    private long activeUserSubscriptions;
    private long activeDieticianSubscriptions;
    private BigDecimal subscriptionRevenue;
    private BigDecimal consultationRevenue;
    private BigDecimal adminCommissionRevenue;
    private long pendingMealCompliance;
    private long followedMealCompliance;
    private long notFollowedMealCompliance;
    private int overallMealCompliancePercent;
    private long totalAppointments;
    private long pendingAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private int weeklyReviewCompletionPercent;
    private int appointmentCompletionPercent;
    private List<AdminActivityMetricResponse> mostActiveDieticians;
    private List<AdminActivityMetricResponse> mostCompliantUsers;
    private List<AdminPaymentResponse> recentPayments;
    private List<AdminUserResponse> recentRegistrations;
}
