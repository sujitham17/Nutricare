package com.nutricare.nutricarebackend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "razorpaySignature")
public class AppointmentPaymentVerifyRequest {

    private Long paymentId;

    private Long appointmentId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;
}
