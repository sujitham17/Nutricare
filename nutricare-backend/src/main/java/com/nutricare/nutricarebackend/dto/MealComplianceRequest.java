package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MealComplianceRequest {

    @NotNull
    private Long mealId;

    @Size(max = 1000)
    private String reason;
}
