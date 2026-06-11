package com.nutricare.nutricarebackend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionConfirmRequest {

    @NotNull
    private Long transactionId;

    @Size(max = 255)
    private String providerPaymentId;

    @NotBlank
    @JsonAlias("razorpay_payment_id")
    private String razorpayPaymentId;

    @NotBlank
    @JsonAlias("razorpay_order_id")
    private String razorpayOrderId;

    @NotBlank
    @JsonAlias("razorpay_signature")
    private String razorpaySignature;
}
