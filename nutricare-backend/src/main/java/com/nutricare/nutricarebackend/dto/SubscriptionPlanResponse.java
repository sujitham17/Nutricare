package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanResponse {

    private Long id;
    private String planName;
    private String name;
    private Role planAudience;
    private String roleType;
    private String description;
    private BigDecimal price;
    private Integer durationInDays;
    private Integer durationDays;
    private String features;
    private boolean active;
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
