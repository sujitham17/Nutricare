package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.DietPlanRequest;
import com.nutricare.nutricarebackend.dto.DietPlanResponse;
import com.nutricare.nutricarebackend.service.DietPlanService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import com.nutricare.nutricarebackend.service.ExcelExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final SubscriptionService subscriptionService;
    private final ExcelExportService excelExportService;

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

    @GetMapping("/my/excel")
    public ResponseEntity<byte[]> getMyPlanExcel(Authentication authentication) {
        subscriptionService.requireFeature(authentication.getName(), "MEAL_LOGS");
        byte[] excelData = excelExportService.myDietPlanReport(authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nutricare-diet-plan.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DietPlanResponse>> getUserDietPlans(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(dietPlanService.getUserDietPlans(authentication.getName(), userId));
    }

}
