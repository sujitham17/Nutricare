package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.DietPlanRequest;
import com.nutricare.nutricarebackend.dto.DietPlanResponse;
import com.nutricare.nutricarebackend.service.DietPlanService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weekly-meal-plans")
@RequiredArgsConstructor
public class WeeklyMealPlanController {

    private final DietPlanService dietPlanService;
    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<DietPlanResponse> create(
            Authentication authentication,
            @Valid @RequestBody DietPlanRequest request
    ) {
        subscriptionService.requireFeature(authentication.getName(), "FOLLOW_UPS");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dietPlanService.createDietPlan(authentication.getName(), request));
    }

    @GetMapping("/dietician")
    public ResponseEntity<List<DietPlanResponse>> getDieticianPlans(Authentication authentication) {
        subscriptionService.requireFeature(authentication.getName(), "FOLLOW_UPS");
        return ResponseEntity.ok(dietPlanService.getDieticianPlans(authentication.getName()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DietPlanResponse>> getUserPlans(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        subscriptionService.requireFeature(authentication.getName(), "FOLLOW_UPS");
        return ResponseEntity.ok(dietPlanService.getUserDietPlans(authentication.getName(), userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<DietPlanResponse>> getMyPlans(Authentication authentication) {
        subscriptionService.requireFeature(authentication.getName(), "FOLLOW_UPS");
        return ResponseEntity.ok(dietPlanService.getMyDietPlans(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<DietPlanResponse>> getAllPlans(Authentication authentication) {
        return ResponseEntity.ok(dietPlanService.getAllPlansForAdmin(authentication.getName()));
    }
}
