package com.nutricare.nutricarebackend.config;

import com.nutricare.nutricarebackend.entity.DieticianProfile;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.DieticianProfileRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DieticianAvailabilitySeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DieticianProfileRepository dieticianProfileRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting DieticianAvailabilitySeeder...");
        List<User> dieticians = userRepository.findByRole(Role.DIETICIAN);
        for (User dietician : dieticians) {
            DieticianProfile profile = dieticianProfileRepository.findByUser(dietician).orElse(null);
            if (profile == null) {
                log.info("Creating default profile with default availability for dietician: {}", dietician.getEmail());
                profile = DieticianProfile.builder()
                        .user(dietician)
                        .specialization(dietician.getSpecialization() != null ? dietician.getSpecialization() : "General Wellness")
                        .experience(dietician.getExperience() != null ? dietician.getExperience() : 0)
                        .qualification(dietician.getDegree() != null ? dietician.getDegree() : "BSc Nutrition")
                        .consultationFee(dietician.getConsultationFee() != null ? dietician.getConsultationFee() : BigDecimal.ZERO)
                        .bio(dietician.getBio())
                        .profileImage(dietician.getProfileImage())
                        .availableFrom(LocalTime.of(9, 0))
                        .availableTo(LocalTime.of(18, 0))
                        .availableDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                        .build();
                dieticianProfileRepository.save(profile);
            } else if (profile.getAvailableFrom() == null || profile.getAvailableTo() == null || profile.getAvailableDays() == null || profile.getAvailableDays().isBlank()) {
                log.info("Setting default availability for dietician: {}", dietician.getEmail());
                profile.setAvailableFrom(LocalTime.of(9, 0));
                profile.setAvailableTo(LocalTime.of(18, 0));
                profile.setAvailableDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
                dieticianProfileRepository.save(profile);
            }
        }
        log.info("DieticianAvailabilitySeeder completed.");
    }
}
