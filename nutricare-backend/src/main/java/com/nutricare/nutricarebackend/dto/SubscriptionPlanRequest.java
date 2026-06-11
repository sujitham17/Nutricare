package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SubscriptionPlanRequest {

    @NotBlank
    @Size(max = 255)
    private String planName;

    private Role planAudience;

    private Role planFor;

    @Size(max = 5000)
    private String description;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    @NotNull
    @Positive
    private Integer durationInDays;

    @Size(max = 5000)
    private String features;

    private boolean active = true;

    private Boolean canBookAppointment;
    private Boolean canVideoCall;
    private Integer videoCallLimitMinutes;
    private Boolean canMealLogs;
    private Boolean canFollowUps;
    private Boolean canChat;

    @Size(max = 500)
    private String allowedUserPlans;

    private Integer maxUsers;
    private Integer maxAppointments;

    public Role getEffectiveAudience() {
        return planAudience == null ? planFor : planAudience;
    }
}
