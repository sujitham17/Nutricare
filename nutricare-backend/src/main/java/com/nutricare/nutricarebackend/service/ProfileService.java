package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.ProfileRequest;
import com.nutricare.nutricarebackend.dto.ProfileResponse;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.DieticianProfileRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final DieticianProfileRepository dieticianProfileRepository;
    private final SubscriptionService subscriptionService;
    private final RatingService ratingService;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    @Value("${nutricare.upload-dir:uploads/profile-images}")
    private String uploadDir;

    public ProfileResponse getProfile(String email) {
        User user = findUser(email);
        syncOnboardingState(user);
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(String email, ProfileRequest request) {
        User user = findUser(email);

        if (hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        user.setPhone(blankToNull(request.getPhone()));
        if (request.getWhatsappNotificationsEnabled() != null) {
            user.setWhatsappNotificationsEnabled(request.getWhatsappNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        user.setAge(request.getAge());
        user.setGender(blankToNull(request.getGender()));
        user.setHeight(request.getHeight());
        user.setWeight(request.getWeight());
        user.setGoal(blankToNull(request.getGoal()));
        user.setBio(blankToNull(request.getBio()));
        user.setSpecialization(blankToNull(request.getSpecialization()));
        user.setBloodPressure(blankToNull(request.getBloodPressure()));
        user.setSugarLevel(blankToNull(request.getSugarLevel()));
        user.setActivityLevel(blankToNull(request.getActivityLevel()));
        user.setDiseaseOrCondition(blankToNull(request.getDiseaseOrCondition()));
        user.setAllergies(blankToNull(request.getAllergies()));
        user.setFoodPreference(blankToNull(request.getFoodPreference()));
        user.setDegree(blankToNull(request.getDegree()));
        user.setExperience(request.getExperience());
        user.setLocation(blankToNull(request.getLocation()));
        user.setConsultationFee(request.getConsultationFee());

        if (hasText(request.getProfileImage())) {
            user.setProfileImage(request.getProfileImage().trim());
        }
        user.setProfileSetupCompleted(isProfileComplete(user));
        user.setOnboardingCompleted(isOnboardingComplete(user, user.isProfileSetupCompleted()));

        User savedUser = userRepository.save(user);
        syncDieticianProfile(savedUser, request);
        auditLogService.record(
                savedUser.getId(),
                savedUser.getRole(),
                "PROFILE_UPDATED",
                "PROFILE",
                "Profile updated for user " + savedUser.getId()
        );
        return toResponse(savedUser);
    }

    @Transactional
    public ProfileResponse updateProfileImage(String email, String imageUrl) {
        User user = findUser(email);
        String previousImageUrl = user.getProfileImage();
        user.setProfileImage(imageUrl);

        User savedUser = userRepository.save(user);
        syncDieticianProfile(savedUser, null);
        deletePreviousUploadedImage(previousImageUrl, imageUrl);
        auditLogService.record(
                savedUser.getId(),
                savedUser.getRole(),
                "PROFILE_UPDATED",
                "PROFILE",
                "Profile image updated for user " + savedUser.getId()
        );
        return toResponse(savedUser);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileResponse toResponse(User user) {
        SubscriptionStatus subscriptionStatus = subscriptionService.getSubscriptionStatus(user);
        boolean subscriptionActive = user.isSubscriptionActive() || subscriptionStatus == SubscriptionStatus.ACTIVE;
        boolean profileCompleted = user.isProfileSetupCompleted();
        boolean subscriptionCompleted = subscriptionActive;
        boolean appointmentCompleted = isAppointmentComplete(user);
        boolean onboardingCompleted = isOnboardingComplete(user, profileCompleted, subscriptionCompleted, appointmentCompleted);
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .subscriptionStatus(subscriptionStatus)
                .createdAt(user.getCreatedAt())
                .phone(user.getPhone())
                .whatsappNotificationsEnabled(user.isWhatsappNotificationsEnabled())
                .smsNotificationsEnabled(user.isSmsNotificationsEnabled())
                .age(user.getAge())
                .gender(user.getGender())
                .height(user.getHeight())
                .weight(user.getWeight())
                .goal(user.getGoal())
                .bio(user.getBio())
                .specialization(user.getSpecialization())
                .profileImage(user.getProfileImage())
                .profileImageUrl(user.getProfileImage())
                .profileSetupCompleted(user.isProfileSetupCompleted())
                .subscriptionActive(subscriptionActive)
                .profileCompleted(profileCompleted)
                .subscriptionCompleted(subscriptionCompleted)
                .hasActiveSubscription(subscriptionCompleted)
                .appointmentCompleted(appointmentCompleted)
                .onboardingCompleted(onboardingCompleted)
                .bloodPressure(user.getBloodPressure())
                .sugarLevel(user.getSugarLevel())
                .activityLevel(user.getActivityLevel())
                .diseaseOrCondition(user.getDiseaseOrCondition())
                .allergies(user.getAllergies())
                .foodPreference(user.getFoodPreference())
                .degree(user.getDegree())
                .experience(user.getExperience())
                .location(user.getLocation())
                .consultationFee(user.getConsultationFee())
                .averageRating(averageRating(user))
                .totalRatings(totalRatings(user));

        if (user.getRole() == Role.DIETICIAN) {
            dieticianProfileRepository.findByUser(user).ifPresent(profile -> {
                builder.bio(firstNonBlank(profile.getBio(), user.getBio()));
                builder.specialization(firstNonBlank(profile.getSpecialization(), user.getSpecialization()));
                String profileImage = firstNonBlank(profile.getProfileImage(), user.getProfileImage());
                builder.profileImage(profileImage);
                builder.profileImageUrl(profileImage);
                builder.degree(firstNonBlank(profile.getQualification(), user.getDegree()));
                builder.experience(profile.getExperience() != null ? profile.getExperience() : user.getExperience());
                builder.consultationFee(profile.getConsultationFee() != null ? profile.getConsultationFee() : user.getConsultationFee());
                builder.availableFrom(profile.getAvailableFrom());
                builder.availableTo(profile.getAvailableTo());
                builder.availableDays(profile.getAvailableDays());
            });
        }

        return builder.build();
    }

    private void syncDieticianProfile(User user, ProfileRequest request) {
        if (user.getRole() != Role.DIETICIAN) {
            return;
        }

        dieticianProfileRepository.findByUser(user).ifPresent(profile -> {
            if (hasText(user.getProfileImage())) {
                profile.setProfileImage(user.getProfileImage());
                dieticianProfileRepository.save(profile);
            }
        });

        if (!isProfileComplete(user)) {
            return;
        }

        dieticianProfileRepository.findByUser(user).ifPresentOrElse(profile -> {
            if (hasText(user.getBio())) {
                profile.setBio(user.getBio());
            }
            if (hasText(user.getSpecialization())) {
                profile.setSpecialization(user.getSpecialization());
            }
            if (hasText(user.getProfileImage())) {
                profile.setProfileImage(user.getProfileImage());
            }
            if (hasText(user.getDegree())) {
                profile.setQualification(user.getDegree());
            }
            if (user.getExperience() != null) {
                profile.setExperience(user.getExperience());
            }
            if (user.getConsultationFee() != null) {
                profile.setConsultationFee(user.getConsultationFee());
            }
            if (request != null) {
                profile.setAvailableFrom(request.getAvailableFrom());
                profile.setAvailableTo(request.getAvailableTo());
                profile.setAvailableDays(request.getAvailableDays());
            }
            dieticianProfileRepository.save(profile);
        }, () -> {
            com.nutricare.nutricarebackend.entity.DieticianProfile.DieticianProfileBuilder builder = com.nutricare.nutricarebackend.entity.DieticianProfile.builder()
                .user(user)
                .specialization(user.getSpecialization())
                .experience(user.getExperience())
                .qualification(user.getDegree())
                .consultationFee(user.getConsultationFee())
                .bio(user.getBio())
                .profileImage(user.getProfileImage());
            if (request != null) {
                builder.availableFrom(request.getAvailableFrom());
                builder.availableTo(request.getAvailableTo());
                builder.availableDays(request.getAvailableDays());
            }
            dieticianProfileRepository.save(builder.build());
        });
    }

    private boolean isProfileComplete(User user) {
        if (!hasText(user.getFullName()) || !hasText(user.getPhone())) {
            return false;
        }
        if (user.getRole() == Role.USER) {
            return user.getAge() != null
                    && hasText(user.getGender())
                    && user.getHeight() != null
                    && user.getWeight() != null
                    && hasText(user.getGoal());
        }
        if (user.getRole() == Role.DIETICIAN) {
            return hasText(user.getDegree())
                    && hasText(user.getSpecialization())
                    && user.getExperience() != null
                    && hasText(user.getLocation())
                    && user.getConsultationFee() != null
                    && user.getConsultationFee().compareTo(BigDecimal.ZERO) >= 0
                    && hasText(user.getBio());
        }
        return true;
    }

    private boolean isOnboardingComplete(User user, boolean profileCompleted) {
        SubscriptionStatus subscriptionStatus = subscriptionService.getSubscriptionStatus(user);
        boolean subscriptionCompleted = user.isSubscriptionActive() || subscriptionStatus == SubscriptionStatus.ACTIVE;
        return isOnboardingComplete(user, profileCompleted, subscriptionCompleted, isAppointmentComplete(user));
    }

    private boolean isOnboardingComplete(
            User user,
            boolean profileCompleted,
            boolean subscriptionCompleted,
            boolean appointmentCompleted
    ) {
        if (user.getRole() == Role.USER || user.getRole() == Role.DIETICIAN) {
            return profileCompleted && subscriptionCompleted && appointmentCompleted;
        }
        return true;
    }

    private boolean isAppointmentComplete(User user) {
        if (user.getRole() == Role.USER) {
            return user.isAppointmentCompleted()
                    || appointmentRepository.existsByUserAndStatusNot(user, AppointmentStatus.CANCELLED);
        }
        if (user.getRole() == Role.DIETICIAN) {
            return user.isAppointmentCompleted()
                    || appointmentRepository.existsByDieticianAndStatusNot(user, AppointmentStatus.CANCELLED);
        }
        return true;
    }

    private void syncOnboardingState(User user) {
        SubscriptionStatus subscriptionStatus = subscriptionService.getSubscriptionStatus(user);
        boolean subscriptionCompleted = user.isSubscriptionActive() || subscriptionStatus == SubscriptionStatus.ACTIVE;
        boolean appointmentCompleted = isAppointmentComplete(user);
        boolean onboardingCompleted = isOnboardingComplete(
                user,
                user.isProfileSetupCompleted(),
                subscriptionCompleted,
                appointmentCompleted
        );

        if (user.isSubscriptionActive() != subscriptionCompleted
                || user.isAppointmentCompleted() != appointmentCompleted
                || user.isOnboardingCompleted() != onboardingCompleted) {
            user.setSubscriptionActive(subscriptionCompleted);
            user.setAppointmentCompleted(appointmentCompleted);
            user.setOnboardingCompleted(onboardingCompleted);
            userRepository.save(user);
        }
    }

    private Double averageRating(User user) {
        return user.getRole() == Role.DIETICIAN ? ratingService.averageRating(user) : null;
    }

    private Long totalRatings(User user) {
        return user.getRole() == Role.DIETICIAN ? ratingService.totalRatings(user) : null;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void deletePreviousUploadedImage(String previousImageUrl, String newImageUrl) {
        if (!hasText(previousImageUrl)
                || previousImageUrl.equals(newImageUrl)
                || !previousImageUrl.startsWith("/uploads/profile-images/")) {
            return;
        }

        String filename = previousImageUrl.substring(previousImageUrl.lastIndexOf('/') + 1);
        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Path imagePath = uploadPath.resolve(filename).normalize();

        if (!imagePath.startsWith(uploadPath)) {
            return;
        }

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException ignored) {
            // Image replacement should not fail because old-file cleanup failed.
        }
    }
}
