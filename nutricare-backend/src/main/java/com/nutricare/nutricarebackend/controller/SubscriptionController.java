package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.SubscriptionConfirmRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionConfirmResponse;
import com.nutricare.nutricarebackend.dto.SubscriptionCreateRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionCreateResponse;
import com.nutricare.nutricarebackend.dto.FeaturePermissionsResponse;
import com.nutricare.nutricarebackend.dto.SubscriptionPlanRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionPlanResponse;
import com.nutricare.nutricarebackend.dto.UserSubscriptionResponse;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping(value = "/api/subscription-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SubscriptionPlanResponse>> getSubscriptionPlans(Authentication authentication) {
        String email = authentication == null ? null : authentication.getName();
        log.info("Entering getSubscriptionPlans. Authenticated user: {}", email != null ? email : "Anonymous");
        try {
            log.info("Query started: Fetching active subscription plans");
            List<SubscriptionPlanResponse> plans = subscriptionService.getActivePlans(email);
            log.info("Query completed. Record count: {}", plans.size());
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Exception fetching active subscription plans: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/api/admin/subscription-plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getAdminSubscriptionPlans() {
        return ResponseEntity.ok(subscriptionService.getAllPlansForAdmin());
    }

    @PostMapping("/api/admin/subscription-plans")
    public ResponseEntity<SubscriptionPlanResponse> createSubscriptionPlan(@Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.createPlanForAdmin(request));
    }

    @PutMapping("/api/admin/subscription-plans/{id}")
    public ResponseEntity<SubscriptionPlanResponse> updateSubscriptionPlan(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.updatePlanForAdmin(id, request));
    }

    @PutMapping("/api/admin/subscription-plans/{id}/deactivate")
    public ResponseEntity<SubscriptionPlanResponse> deactivateSubscriptionPlan(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.deactivatePlanForAdmin(id));
    }

    @PatchMapping("/api/admin/subscription-plans/{id}/deactivate")
    public ResponseEntity<SubscriptionPlanResponse> patchDeactivateSubscriptionPlan(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.deactivatePlanForAdmin(id));
    }

    @PatchMapping("/api/admin/subscription-plans/{id}/activate")
    public ResponseEntity<SubscriptionPlanResponse> activateSubscriptionPlan(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.activatePlanForAdmin(id));
    }

    @DeleteMapping("/api/admin/subscription-plans/{id}")
    public ResponseEntity<Void> deleteSubscriptionPlan(@PathVariable Long id) {
        subscriptionService.deletePlanForAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/subscriptions/create")
    public ResponseEntity<SubscriptionCreateResponse> createSubscription(
            Authentication authentication,
            @Valid @RequestBody SubscriptionCreateRequest request
    ) {
        log.info("Subscription create authenticated email: {}", authentication.getName());
        log.info("Subscription create authorities: {}", authentication.getAuthorities());
        log.info("Subscription create planId received: {}", request.getPlanId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscriptionTransaction(authentication.getName(), request));
    }

    @PostMapping("/api/subscriptions/confirm")
    public ResponseEntity<SubscriptionConfirmResponse> confirmSubscription(
            Authentication authentication,
            @Valid @RequestBody SubscriptionConfirmRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.confirmSubscription(authentication.getName(), request));
    }

    @GetMapping(value = "/api/subscriptions/my", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserSubscriptionResponse> getMySubscription(Authentication authentication) {
        String email = authentication == null ? null : authentication.getName();
        log.info("Entering getMySubscription. Authenticated user: {}", email != null ? email : "Anonymous");
        try {
            if (email == null) {
                return ResponseEntity.ok(UserSubscriptionResponse.builder()
                        .status(com.nutricare.nutricarebackend.entity.SubscriptionStatus.INACTIVE)
                        .build());
            }
            log.info("Query started: Fetching current subscription for user: {}", email);
            UserSubscriptionResponse sub = subscriptionService.getMySubscription(email);
            log.info("Query completed for getMySubscription. Subscription status: {}", sub != null ? sub.getStatus() : "None");
            return ResponseEntity.ok(sub);
        } catch (Exception e) {
            log.error("Exception in getMySubscription: {}", e.getMessage(), e);
            return ResponseEntity.ok(UserSubscriptionResponse.builder()
                    .status(com.nutricare.nutricarebackend.entity.SubscriptionStatus.INACTIVE)
                    .build());
        }
    }

    @GetMapping(value = "/api/subscriptions/my-features", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FeaturePermissionsResponse> getMyFeatures(Authentication authentication) {
        String email = authentication == null ? null : authentication.getName();
        log.info("Entering getMyFeatures. Authenticated user: {}", email != null ? email : "Anonymous");
        try {
            log.info("Query started: Fetching features/permissions for user: {}", email);
            FeaturePermissionsResponse response = subscriptionService.getMyFeatures(email);
            log.info("Query completed for getMyFeatures.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getMyFeatures: {}", e.getMessage(), e);
            return ResponseEntity.ok(FeaturePermissionsResponse.builder()
                    .subscriptionStatus(com.nutricare.nutricarebackend.entity.SubscriptionStatus.INACTIVE)
                    .canBookAppointment(false)
                    .canVideoCall(false)
                    .videoCallLimitMinutes(0)
                    .canMealLogs(false)
                    .canFollowUps(false)
                    .canChat(false)
                    .canHealthTracking(true)
                    .build());
        }
    }
}
