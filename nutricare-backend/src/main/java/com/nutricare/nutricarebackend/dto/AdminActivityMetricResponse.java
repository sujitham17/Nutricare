package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminActivityMetricResponse {

    private Long accountId;
    private String fullName;
    private long count;
    private int percentage;
}
