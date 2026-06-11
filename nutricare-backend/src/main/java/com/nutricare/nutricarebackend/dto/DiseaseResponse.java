package com.nutricare.nutricarebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseResponse {
    private Long id;
    private String name;
    private String overview;
    private List<String> symptoms;
    private List<String> recommendedFoods;
    private List<String> foodsToAvoid;
    private String nutritionTips;
    private String status;
    private String icon;
    private String color;
    private String bg;
    private String image;
    private String description;
}
