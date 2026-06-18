package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SubscriptionPlanRequest {

    @Size(max = 255)
    private String planName;

    private String name;

    private Role planAudience;

    private String roleType;

    private Role planFor;

    @Size(max = 5000)
    private String description;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    private Integer durationInDays;

    private Integer durationDays;

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

    public String getPlanName() {
        return planName != null ? planName : name;
    }

    public String getName() {
        return name != null ? name : planName;
    }

    public Role getPlanAudience() {
        if (planAudience != null) return planAudience;
        if (roleType != null) {
            try {
                return Role.valueOf(roleType.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public String getRoleType() {
        if (roleType != null) return roleType;
        return planAudience != null ? planAudience.name() : null;
    }

    public Integer getDurationInDays() {
        return durationInDays != null ? durationInDays : durationDays;
    }

    public Integer getDurationDays() {
        return durationDays != null ? durationDays : durationInDays;
    }

    public Role getEffectiveAudience() {
        Role audience = getPlanAudience();
        return audience == null ? planFor : audience;
    }
}
