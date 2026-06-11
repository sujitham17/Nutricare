package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.MealComplianceRejectRequest;
import com.nutricare.nutricarebackend.dto.MealComplianceRequest;
import com.nutricare.nutricarebackend.dto.MealComplianceResponse;
import com.nutricare.nutricarebackend.entity.MealComplianceStatus;
import com.nutricare.nutricarebackend.service.MealComplianceService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MealComplianceController {

    private final MealComplianceService mealComplianceService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/api/meal-compliance/followed")
    public ResponseEntity<MealComplianceResponse> markFollowed(
            Authentication authentication,
            @Valid @RequestBody MealComplianceRequest request
    ) {
        subscriptionService.requireFeature(authentication.getName(), "MEAL_LOGS");
        return ResponseEntity.ok(mealComplianceService.markFollowed(authentication.getName(), request));
    }

    @PostMapping("/api/meal-compliance/not-followed")
    public ResponseEntity<MealComplianceResponse> markNotFollowed(
            Authentication authentication,
            @Valid @RequestBody MealComplianceRejectRequest request
    ) {
        subscriptionService.requireFeature(authentication.getName(), "MEAL_LOGS");
        return ResponseEntity.ok(mealComplianceService.markNotFollowed(authentication.getName(), request));
    }

    @GetMapping("/api/meal-compliance/my")
    public ResponseEntity<List<MealComplianceResponse>> getMyCompliance(Authentication authentication) {
        subscriptionService.requireFeature(authentication.getName(), "MEAL_LOGS");
        return ResponseEntity.ok(mealComplianceService.getMyCompliance(authentication.getName()));
    }

    @GetMapping("/api/meal-compliance/user/{userId}")
    public ResponseEntity<List<MealComplianceResponse>> getUserCompliance(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        subscriptionService.requireFeature(authentication.getName(), "FOLLOW_UPS");
        return ResponseEntity.ok(mealComplianceService.getUserCompliance(authentication.getName(), userId));
    }

    @GetMapping("/api/meal-compliance/dietician")
    public ResponseEntity<List<MealComplianceResponse>> getDieticianCompliance(
            Authentication authentication,
            @RequestParam(required = false) MealComplianceStatus status
    ) {
        subscriptionService.requireFeature(authentication.getName(), "FOLLOW_UPS");
        return ResponseEntity.ok(mealComplianceService.getDieticianCompliance(authentication.getName(), status));
    }
}
