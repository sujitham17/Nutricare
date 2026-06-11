package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminSubscriptionResponse {

    private Long id;
    private Long accountId;
    private String fullName;
    private String userName;
    private String email;
    private Role role;
    private Long planId;
    private String planName;
    private BigDecimal price;
    private BigDecimal amount;
    private SubscriptionStatus status;
    private SubscriptionStatus subscriptionStatus;
    private PaymentStatus paymentStatus;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
