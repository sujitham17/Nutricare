package com.nutricare.nutricarebackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequest {

    private String fullName;
    private String phone;
    private Boolean whatsappNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Integer age;
    private String gender;
    private Double height;
    private Double weight;
    private String goal;
    private String bio;
    private String specialization;
    private String profileImage;
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
    private java.time.LocalTime availableFrom;
    private java.time.LocalTime availableTo;
    private String availableDays;
}
