package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordTokenRequest {

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 6)
    private String newPassword;
}
