package com.nutricare.nutricarebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "diseases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "recommendedFoods", columnDefinition = "TEXT")
    private String recommendedFoods;

    @Column(name = "foodsToAvoid", columnDefinition = "TEXT")
    private String foodsToAvoid;

    @Column(name = "nutritionTips", columnDefinition = "TEXT")
    private String nutritionTips;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
    private String status = "ACTIVE";

    @Column(length = 1000)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(length = 50)
    private String bg;

    @Column(length = 1000)
    private String image;

    @Column(length = 1000)
    private String description;

    @Column(name = "createdAt", nullable = false, updatable = false, columnDefinition = "datetime(6) default current_timestamp(6)")
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
