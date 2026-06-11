package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPaymentResponse {

    private Long id;
    private String paymentType;
    private Long payerId;
    private String payerName;
    private String payerEmail;
    private Role payerRole;
    private String planName;
    private BigDecimal amount;
    private BigDecimal adminCommission;
    private BigDecimal dieticianEarnings;
    private PaymentStatus paymentStatus;
    private String paymentStatusText;
    private LocalDateTime createdAt;
}
