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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weekly-meal-plans")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class WeeklyMealPlanController {

    private final DietPlanService dietPlanService;
    private final SubscriptionService subscriptionService;
    private final ExcelExportService excelExportService;
    private final com.nutricare.nutricarebackend.repository.UserRepository userRepository;

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

    @GetMapping("/my/excel")
    public ResponseEntity<byte[]> getMyPlanExcel(Authentication authentication) {
        log.info("getMyPlanExcel endpoint called for: {}", authentication.getName());
        try {
            com.nutricare.nutricarebackend.entity.User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
            log.info("getMyPlanExcel authenticated user id: {}, email: {}, role: {}, export type: my-diet-plan-excel",
                    user.getId(), user.getEmail(), user.getRole().name());
            subscriptionService.requireFeature(authentication.getName(), "MEAL_LOGS");
            byte[] excelData = excelExportService.myDietPlanReport(authentication.getName());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nutricare-diet-plan.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            log.error("ResponseStatusException in getMyPlanExcel: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Throwable ex) {
            log.error("Throwable in getMyPlanExcel: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @GetMapping
    public ResponseEntity<List<DietPlanResponse>> getAllPlans(Authentication authentication) {
        return ResponseEntity.ok(dietPlanService.getAllPlansForAdmin(authentication.getName()));
    }
}
