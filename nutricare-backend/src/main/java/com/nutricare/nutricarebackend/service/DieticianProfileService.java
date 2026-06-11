package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.DieticianProfileRequest;
import com.nutricare.nutricarebackend.dto.DieticianProfileResponse;
import com.nutricare.nutricarebackend.entity.DieticianProfile;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.repository.DieticianProfileRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DieticianProfileService {

    private final DieticianProfileRepository dieticianProfileRepository;
    private final UserRepository userRepository;
    private final RatingService ratingService;

    public List<DieticianProfileResponse> getAllDieticians() {
        return dieticianProfileRepository.findAll()
                .stream()
                .filter(profile -> profile.getUser().getStatus() == UserStatus.ACTIVE)
                .map(this::toResponse)
                .toList();
    }

    public DieticianProfileResponse getDieticianById(Long id) {
        DieticianProfile profile = dieticianProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dietician profile not found"));
        if (profile.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dietician profile not found");
        }

        return toResponse(profile);
    }

    public DieticianProfileResponse createProfile(String email, DieticianProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only dieticians can create a profile");
        }

        if (dieticianProfileRepository.existsByUser(user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dietician profile already exists");
        }

        DieticianProfile profile = DieticianProfile.builder()
                .user(user)
                .specialization(request.getSpecialization())
                .experience(request.getExperience())
                .qualification(request.getQualification())
                .consultationFee(request.getConsultationFee())
                .bio(request.getBio())
                .profileImage(request.getProfileImage())
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .availableDays(request.getAvailableDays())
                .build();

        return toResponse(dieticianProfileRepository.save(profile));
    }

    private DieticianProfileResponse toResponse(DieticianProfile profile) {
        User user = profile.getUser();
        String profileImage = firstNonBlank(profile.getProfileImage(), user.getProfileImage());

        return DieticianProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .specialization(profile.getSpecialization())
                .experience(profile.getExperience())
                .qualification(profile.getQualification())
                .consultationFee(profile.getConsultationFee())
                .bio(profile.getBio())
                .profileImage(profileImage)
                .profileImageUrl(profileImage)
                .degree(profile.getQualification())
                .location(user.getLocation())
                .averageRating(ratingService.averageRating(user))
                .totalRatings(ratingService.totalRatings(user))
                .availableFrom(profile.getAvailableFrom())
                .availableTo(profile.getAvailableTo())
                .availableDays(profile.getAvailableDays())
                .build();
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
