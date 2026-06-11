package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.HealthTrackingRequest;
import com.nutricare.nutricarebackend.dto.HealthTrackingResponse;
import com.nutricare.nutricarebackend.dto.UserHealthProfileResponse;
import com.nutricare.nutricarebackend.service.HealthTrackingService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HealthTrackingController {

    private final HealthTrackingService healthTrackingService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/api/health-tracking")
    public ResponseEntity<HealthTrackingResponse> create(
            Authentication authentication,
            @Valid @RequestBody HealthTrackingRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(healthTrackingService.create(authentication.getName(), request));
    }

    @GetMapping("/api/health-tracking/my")
    public ResponseEntity<List<HealthTrackingResponse>> getMyRecords(Authentication authentication) {
        return ResponseEntity.ok(healthTrackingService.getMyRecords(authentication.getName()));
    }

    @PutMapping("/api/health-tracking/{id}")
    public ResponseEntity<HealthTrackingResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody HealthTrackingRequest request
    ) {
        return ResponseEntity.ok(healthTrackingService.update(authentication.getName(), id, request));
    }

    @GetMapping("/api/health-tracking/user/{userId}")
    public ResponseEntity<List<HealthTrackingResponse>> getUserRecords(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(healthTrackingService.getUserRecords(authentication.getName(), userId));
    }

    @GetMapping("/api/health-profile/user/{userId}")
    public ResponseEntity<UserHealthProfileResponse> getUserHealthProfile(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(healthTrackingService.getUserHealthProfile(authentication.getName(), userId));
    }
}
