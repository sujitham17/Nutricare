package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionCreateRequest {

    @NotNull
    private Long planId;

    @NotNull
    private Long userId;

    @Size(max = 255)
    private String paymentProvider;
}
