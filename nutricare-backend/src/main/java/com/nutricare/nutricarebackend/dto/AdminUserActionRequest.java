package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserActionRequest {

    @Size(max = 1000)
    private String reason;
}
