package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
    private SubscriptionStatus subscriptionStatus;
    private String profileImage;
    private String profileImageUrl;
    private String actionReason;
    private String rejectionReason;
    private Long actionBy;
    private LocalDateTime actionDate;
    private LocalDateTime adminActionAt;
    private LocalDateTime createdAt;
}
