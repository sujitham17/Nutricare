package com.nutricare.nutricarebackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultationPaymentCreateOrderRequest {

    private Long appointmentId;

    private Long planId;

    private Long userId;

    private String paymentType;
}
