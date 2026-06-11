package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportSnapshotRequest {

    @NotBlank
    private String reportType;

    private Long userId;
    private Long dieticianId;
    private Object data;
}
