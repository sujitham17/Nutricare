package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ConsultationPaymentCreateOrderResponse {

    private Long appointmentId;
    private Long transactionId;
    private Long planId;
    private Long userId;
    private String paymentType;
    private BigDecimal amount;
    private String currency;
    private Long paymentId;
    private String orderId;
    private String razorpayOrderId;
    private String key;
    private PaymentStatus paymentStatus;
}
