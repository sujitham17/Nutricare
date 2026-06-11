package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPasswordOtpRequest {

    @NotBlank
    private String identifier;

    @NotBlank
    private String otp;
}
