package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultationPaymentConfirmRequest {

    @NotNull
    private Long paymentId;

    @Size(max = 255)
    private String providerPaymentId;

    @NotNull
    private PaymentStatus paymentStatus;
}
