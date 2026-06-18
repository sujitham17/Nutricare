package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Slf4j
public class DevController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${nutricare.dev.endpoints.enabled:false}")
    private boolean devEndpointsEnabled;

    @PostMapping("/reset-admin-password")
    public ResponseEntity<Map<String, Object>> resetAdminPassword() {
        if (!devEndpointsEnabled) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Endpoint disabled");
        }
        log.info("Temporary admin password reset endpoint invoked.");
        
        User user = userRepository.findByEmail("nutricareadmin@gmail.com")
                .orElseGet(() -> {
                    log.info("Admin user not found. Creating a new one.");
                    return User.builder()
                            .email("nutricareadmin@gmail.com")
                            .fullName("NutriCare Admin")
                            .provider("LOCAL")
                            .profileSetupCompleted(false)
                            .subscriptionActive(false)
                            .appointmentCompleted(false)
                            .onboardingCompleted(false)
                            .build();
                });

        user.setPassword(passwordEncoder.encode("87654321"));
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        userRepository.save(user);
        log.info("Successfully reset password and updated admin details for nutricareadmin@gmail.com.");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Admin password reset successfully."
        ));
    }
}
