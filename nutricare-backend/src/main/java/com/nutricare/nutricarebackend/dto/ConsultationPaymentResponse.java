package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ConsultationPaymentResponse {

    private Boolean success;
    private String message;
    private Long id;
    private Long appointmentId;
    private AppointmentStatus appointmentStatus;
    private Long userId;
    private String userFullName;
    private Long dieticianId;
    private String dieticianFullName;
    private BigDecimal amount;
    private BigDecimal platformCommissionRate;
    private BigDecimal platformCommissionAmount;
    private BigDecimal dieticianEarningsAmount;
    private String currency;
    private PaymentStatus paymentStatus;
    private String paymentStatusText;
    private String paymentProvider;
    private String providerPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;
    private java.time.LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
