package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class DieticianProfileRequest {

    @NotBlank
    private String specialization;

    @NotNull
    @Min(0)
    private Integer experience;

    @NotBlank
    private String qualification;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal consultationFee;

    private String bio;

    private String profileImage;

    private LocalTime availableFrom;

    private LocalTime availableTo;

    private String availableDays;
}
