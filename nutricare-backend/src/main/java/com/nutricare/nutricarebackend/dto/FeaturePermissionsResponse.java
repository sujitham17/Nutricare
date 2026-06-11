package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturePermissionsResponse {

    private Long userId;
    private Role role;
    private Long planId;
    private String planName;
    private SubscriptionStatus subscriptionStatus;
    private boolean canBookAppointment;
    private boolean canVideoCall;
    private Integer videoCallLimitMinutes;
    private boolean canMealLogs;
    private boolean canFollowUps;
    private boolean canChat;
    private String allowedUserPlans;
    private Integer maxUsers;
}
