package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.UserStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminCreateDieticianRequest {

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotBlank
    private String degree;

    @NotBlank
    private String specialization;

    @NotNull
    @Min(0)
    private Integer experience;

    @NotBlank
    private String location;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal consultationFee;

    @NotNull
    private UserStatus status = UserStatus.ACTIVE;
}
