package com.nutricare.nutricarebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private boolean active;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "planName")
    private String name;

    @Column(name = "planAudience")
    private String roleType;

    @Column(name = "duration")
    private Integer durationDays;

    @Column(name = "allowed_user_plans")
    private String allowedUserPlans;

    @Column(name = "can_book_appointment")
    private Boolean canBookAppointment;

    @Column(name = "can_chat")
    private Boolean canChat;

    @Column(name = "can_follow_ups")
    private Boolean canFollowUps;

    @Column(name = "can_meal_logs")
    private Boolean canMealLogs;

    @Column(name = "can_video_call")
    private Boolean canVideoCall;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "video_call_limit_minutes")
    private Integer videoCallLimitMinutes;

    @Column(name = "max_appointments")
    private Integer maxAppointments;
}
