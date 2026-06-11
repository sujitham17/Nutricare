package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime createdAt;
    private String phone;
    private boolean whatsappNotificationsEnabled;
    private boolean smsNotificationsEnabled;
    private Integer age;
    private String gender;
    private Double height;
    private Double weight;
    private String goal;
    private String bio;
    private String specialization;
    private String profileImage;
    private String profileImageUrl;
    private boolean profileSetupCompleted;
    private boolean subscriptionActive;
    private boolean profileCompleted;
    private boolean subscriptionCompleted;
    private boolean hasActiveSubscription;
    private boolean appointmentCompleted;
    private boolean onboardingCompleted;
    private String bloodPressure;
    private String sugarLevel;
    private String activityLevel;
    private String diseaseOrCondition;
    private String allergies;
    private String foodPreference;
    private String degree;
    private Integer experience;
    private String location;
    private java.math.BigDecimal consultationFee;
    private Double averageRating;
    private Long totalRatings;
    private java.time.LocalTime availableFrom;
    private java.time.LocalTime availableTo;
    private String availableDays;
}
