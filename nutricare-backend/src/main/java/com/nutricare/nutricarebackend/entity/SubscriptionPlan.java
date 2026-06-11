package com.nutricare.nutricarebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String planName;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(20) default 'USER'")
    @Enumerated(EnumType.STRING)
    private Role planAudience = Role.USER;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer durationInDays;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(nullable = false)
    private boolean active;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean canBookAppointment = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean canVideoCall = false;

    private Integer videoCallLimitMinutes;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean canMealLogs = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean canFollowUps = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean canChat = false;

    @Column(length = 500)
    private String allowedUserPlans;

    private Integer maxUsers;

    @Column(name = "max_appointments")
    private Integer maxAppointments;
}
