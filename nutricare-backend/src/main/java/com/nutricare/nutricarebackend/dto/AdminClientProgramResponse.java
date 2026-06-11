package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AdminClientProgramResponse {

    private Long dietPlanId;
    private Long dieticianId;
    private String dieticianName;
    private Long userId;
    private String userName;
    private String programGoal;
    private LocalDate startDate;
    private Integer currentProgress;
}
