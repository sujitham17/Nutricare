package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.DietPlanRequest;
import com.nutricare.nutricarebackend.dto.DietPlanResponse;
import com.nutricare.nutricarebackend.service.DietPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@RestController
@RequestMapping("/api/diet-plans")
@RequiredArgsConstructor
public class DietPlanController {

    private final DietPlanService dietPlanService;

    @PostMapping
    public ResponseEntity<DietPlanResponse> createDietPlan(
            Authentication authentication,
            @Valid @RequestBody DietPlanRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dietPlanService.createDietPlan(authentication.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<DietPlanResponse>> getMyDietPlans(Authentication authentication) {
        return ResponseEntity.ok(dietPlanService.getMyDietPlans(authentication.getName()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DietPlanResponse>> getUserDietPlans(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(dietPlanService.getUserDietPlans(authentication.getName(), userId));
    }

}
