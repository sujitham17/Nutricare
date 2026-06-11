package com.nutricare.nutricarebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminRevenueResponse {

    private BigDecimal subscriptionRevenue;
    private BigDecimal consultationRevenue;
    private BigDecimal adminCommissionRevenue;
    private BigDecimal dieticianEarningsPaid;
    private BigDecimal totalRevenue;
}
