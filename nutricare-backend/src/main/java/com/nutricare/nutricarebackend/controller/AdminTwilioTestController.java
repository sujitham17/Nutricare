package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.TwilioTestRequest;
import com.nutricare.nutricarebackend.service.TwilioNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTwilioTestController {

    private final TwilioNotificationService twilioNotificationService;

    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, Object>> testSms(@Valid @RequestBody TwilioTestRequest request) {
        var result = twilioNotificationService.sendTestSms(request.getTo(), request.getMessage());
        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sid", result.sid()
            ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", result.errorMessage() != null ? result.errorMessage() : "Twilio send failed"
        ));
    }
}
