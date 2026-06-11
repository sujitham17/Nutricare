package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionResponse {

    private Long id;
    private Long userId;
    private Long planId;
    private String planName;
    private Role planAudience;
    private String planDescription;
    private BigDecimal price;
    private Integer durationInDays;
    private String features;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private boolean canBookAppointment;
    private boolean canVideoCall;
    private Integer videoCallLimitMinutes;
    private boolean canMealLogs;
    private boolean canFollowUps;
    private boolean canChat;
    private String allowedUserPlans;
    private Integer maxUsers;
    private Integer maxAppointments;
}
