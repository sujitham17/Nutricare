package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.PaymentStatus;
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
public class PaymentTransactionResponse {

    private Long id;
    private Long userId;
    private Long planId;
    private String planName;
    private Role planAudience;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String paymentStatusText;
    private SubscriptionStatus subscriptionStatus;
    private String paymentProvider;
    private String providerPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
