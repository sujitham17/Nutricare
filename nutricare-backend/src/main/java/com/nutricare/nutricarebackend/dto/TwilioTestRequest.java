package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TwilioTestRequest {

    @NotBlank
    private String to;

    @NotBlank
    private String message;
}
