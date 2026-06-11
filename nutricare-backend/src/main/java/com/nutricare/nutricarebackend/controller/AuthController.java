package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.AuthResponse;
import com.nutricare.nutricarebackend.dto.ForgotPasswordOtpRequest;
import com.nutricare.nutricarebackend.dto.GoogleLoginRequest;
import com.nutricare.nutricarebackend.dto.LoginRequest;
import com.nutricare.nutricarebackend.dto.RegisterRequest;
import com.nutricare.nutricarebackend.dto.ResetPasswordOtpRequest;
import com.nutricare.nutricarebackend.dto.VerifyPasswordOtpRequest;
import com.nutricare.nutricarebackend.dto.RegisterSendOtpRequest;
import com.nutricare.nutricarebackend.dto.RegisterVerifyRequest;
import com.nutricare.nutricarebackend.dto.ResetPasswordTokenRequest;
import com.nutricare.nutricarebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @PostMapping("/register/send-otp")
    public ResponseEntity<java.util.Map<String, Object>> sendRegisterOtp(@Valid @RequestBody RegisterSendOtpRequest request) {
        try {
            userService.sendRegisterOtp(request);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "OTP sent successfully to your Gmail."));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unable to send registration OTP: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of(
                            "success", false,
                            "message", "Unable to send registration OTP"
                    ));
        }
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<AuthResponse> verifyRegisterOtp(@Valid @RequestBody RegisterVerifyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.verifyRegisterOtp(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<java.util.Map<String, Object>> sendForgotPasswordOtp(@Valid @RequestBody ForgotPasswordOtpRequest request) {
        userService.sendForgotPasswordOtp(request);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "If this Gmail exists, an OTP has been sent."));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<java.util.Map<String, Object>> verifyForgotPasswordOtp(@Valid @RequestBody VerifyPasswordOtpRequest request) {
        String resetToken = userService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(java.util.Map.of("success", true, "resetToken", resetToken, "message", "OTP verified successfully."));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<java.util.Map<String, Object>> resetForgotPassword(@Valid @RequestBody ResetPasswordTokenRequest request) {
        userService.resetForgotPassword(request);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Password reset successfully. Please login."));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(userService.googleLogin(request));
    }
}
