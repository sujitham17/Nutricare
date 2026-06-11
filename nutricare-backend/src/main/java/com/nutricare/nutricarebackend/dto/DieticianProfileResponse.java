package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DieticianProfileResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private String specialization;
    private Integer experience;
    private String qualification;
    private BigDecimal consultationFee;
    private String bio;
    private String profileImage;
    private String profileImageUrl;
    private String degree;
    private String location;
    private Double averageRating;
    private Long totalRatings;
    private LocalTime availableFrom;
    private LocalTime availableTo;
    private String availableDays;
}
