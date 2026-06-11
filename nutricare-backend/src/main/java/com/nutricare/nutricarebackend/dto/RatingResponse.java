package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RatingResponse {

    private Long id;
    private Long appointmentId;
    private Long userId;
    private Long dieticianId;
    private Integer rating;
    private String review;
    private LocalDateTime createdAt;
}
