package com.nutricare.nutricarebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsResponse {
    private double storageUsedGb;
    private double storageTotalGb;
    private double storageUsedPercent;
}
