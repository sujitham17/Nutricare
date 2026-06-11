package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
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
public class AuthResponse {

    private String message;
    private String token;
    private Long userId;
    private Role role;
    private Boolean onboardingCompleted;
    private Boolean subscriptionCompleted;
    private Boolean hasActiveSubscription;
    private String profileImageUrl;
    private UserResponse user;
}
