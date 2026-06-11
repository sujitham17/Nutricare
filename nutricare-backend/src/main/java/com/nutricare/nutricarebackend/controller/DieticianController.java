package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.DieticianProfileRequest;
import com.nutricare.nutricarebackend.dto.DieticianProfileResponse;
import com.nutricare.nutricarebackend.dto.AvailableSlotResponse;
import com.nutricare.nutricarebackend.service.AppointmentService;
import com.nutricare.nutricarebackend.service.DieticianProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DieticianController {

    private final DieticianProfileService dieticianProfileService;
    private final AppointmentService appointmentService;

    @GetMapping("/api/dieticians")
    public ResponseEntity<List<DieticianProfileResponse>> getAllDieticians() {
        return ResponseEntity.ok(dieticianProfileService.getAllDieticians());
    }

    @GetMapping("/api/dieticians/{id}")
    public ResponseEntity<DieticianProfileResponse> getDieticianById(@PathVariable Long id) {
        return ResponseEntity.ok(dieticianProfileService.getDieticianById(id));
    }

    @GetMapping("/api/dieticians/{id}/available-slots")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(id, date));
    }

    @PostMapping("/api/dietician/profile")
    public ResponseEntity<DieticianProfileResponse> createProfile(
            Authentication authentication,
            @Valid @RequestBody DieticianProfileRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dieticianProfileService.createProfile(authentication.getName(), request));
    }
}
