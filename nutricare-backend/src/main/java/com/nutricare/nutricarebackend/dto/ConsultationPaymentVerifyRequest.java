package com.nutricare.nutricarebackend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultationPaymentVerifyRequest {

    private Long paymentId;

    private Long appointmentId;

    private Long transactionId;

    private Long planId;

    private Long userId;

    private String paymentType;

    @JsonProperty("razorpay_payment_id")
    @JsonAlias("razorpayPaymentId")
    private String razorpayPaymentId;

    @JsonProperty("razorpay_order_id")
    @JsonAlias("razorpayOrderId")
    private String razorpayOrderId;

    @JsonProperty("razorpay_signature")
    @JsonAlias("razorpaySignature")
    private String razorpaySignature;
}
