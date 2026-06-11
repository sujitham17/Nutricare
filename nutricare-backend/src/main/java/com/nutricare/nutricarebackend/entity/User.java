package com.nutricare.nutricarebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false, updatable = false, columnDefinition = "datetime(6) default current_timestamp(6)")
    private LocalDateTime createdAt;

    @Column(length = 1000)
    private String actionReason;

    @Column(length = 1000)
    private String rejectionReason;

    private Long actionBy;

    private LocalDateTime actionDate;

    private LocalDateTime adminActionAt;

    private String phone;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 1")
    private boolean whatsappNotificationsEnabled = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 1")
    private boolean smsNotificationsEnabled = true;

    private Integer age;

    private String gender;

    private Double height;

    private Double weight;

    private String goal;

    @Column(length = 1000)
    private String bio;

    private String specialization;

    @Column(length = 1000)
    private String profileImage;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean profileSetupCompleted = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean subscriptionActive = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean appointmentCompleted = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean onboardingCompleted = false;

    private String bloodPressure;

    @Column(length = 50)
    private String sugarLevel;

    private String activityLevel;

    private String diseaseOrCondition;

    @Column(length = 1000)
    private String allergies;

    private String foodPreference;

    private String degree;

    private Integer experience;

    private String location;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean emailVerified = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(50) default 'LOCAL'")
    private String provider = "LOCAL";

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (provider == null) {
            provider = "LOCAL";
        }
    }
}
