package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.HealthTrackingRequest;
import com.nutricare.nutricarebackend.dto.HealthTrackingResponse;
import com.nutricare.nutricarebackend.dto.UserHealthProfileResponse;
import com.nutricare.nutricarebackend.entity.HealthTracking;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.HealthTrackingRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthTrackingService {

    private final HealthTrackingRepository healthTrackingRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    public HealthTrackingResponse create(String email, HealthTrackingRequest request) {
        User user = getUserByEmail(email);
        accessControlService.requireRole(user, Role.USER, "Only users can create health records");

        HealthTracking record = HealthTracking.builder()
                .user(user)
                .build();
        apply(record, request);
        return toResponse(healthTrackingRepository.save(record));
    }

    public List<HealthTrackingResponse> getMyRecords(String email) {
        User user = getUserByEmail(email);
        accessControlService.requireRole(user, Role.USER, "Only users can view their health records");

        List<HealthTracking> records = healthTrackingRepository.findByUserOrderByRecordedDateDescCreatedAtDesc(user);
        if (!records.isEmpty()) {
            return records.stream()
                    .map(this::toResponse)
                    .toList();
        }

        HealthTrackingResponse profileDto = HealthTrackingResponse.builder()
                .id(null)
                .userId(user.getId())
                .userFullName(user.getFullName())
                .age(user.getAge())
                .gender(user.getGender())
                .height(toBigDecimal(user.getHeight()))
                .weight(toBigDecimal(user.getWeight()))
                .bmi(calculateBmi(toBigDecimal(user.getWeight()), toBigDecimal(user.getHeight())))
                .activityLevel(user.getActivityLevel())
                .goal(user.getGoal())
                .recordedDate(LocalDate.now())
                .build();

        return List.of(profileDto);
    }

    public HealthTrackingResponse update(String email, Long id, HealthTrackingRequest request) {
        User user = getUserByEmail(email);
        accessControlService.requireRole(user, Role.USER, "Only users can update health records");

        HealthTracking record = healthTrackingRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Health record not found"));
        apply(record, request);
        return toResponse(healthTrackingRepository.save(record));
    }

    public List<HealthTrackingResponse> getUserRecords(String email, Long userId) {
        User dietician = getUserByEmail(email);
        User user = getUserById(userId);
        accessControlService.requireAssignedDietician(dietician, user);

        return healthTrackingRepository.findByUserOrderByRecordedDateDescCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserHealthProfileResponse getUserHealthProfile(String email, Long userId) {
        User requester = getUserByEmail(email);
        User user = getUserById(userId);

        if (requester.getRole() == Role.DIETICIAN) {
            accessControlService.requireAssignedDietician(requester, user);
        } else if (requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only assigned dieticians or admins can view health profile");
        }

        HealthTracking latest = healthTrackingRepository.findFirstByUserOrderByRecordedDateDescCreatedAtDesc(user)
                .orElse(null);

        return UserHealthProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .age(user.getAge())
                .gender(user.getGender())
                .height(latest == null ? toBigDecimal(user.getHeight()) : latest.getHeight())
                .weight(latest == null ? toBigDecimal(user.getWeight()) : latest.getWeight())
                .bmi(latest == null ? null : latest.getBmi())
                .goal(user.getGoal())
                .activityLevel(latest == null ? user.getActivityLevel() : latest.getActivityLevel())
                .allergies(user.getAllergies())
                .foodPreference(user.getFoodPreference())
                .notes(latest == null ? null : latest.getNotes())
                .medicalConditions(user.getDiseaseOrCondition())
                .specialCondition(user.getDiseaseOrCondition())
                .latestRecordedDate(latest == null ? null : latest.getRecordedDate())
                .build();
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private void apply(HealthTracking record, HealthTrackingRequest request) {
        record.setWeight(request.getWeight());
        record.setHeight(request.getHeight());
        record.setBmi(request.getBmi() == null ? calculateBmi(request.getWeight(), request.getHeight()) : request.getBmi());
        record.setBloodPressure(request.getBloodPressure());
        record.setSugarLevel(request.getSugarLevel());
        record.setWaterIntake(request.getWaterIntake());
        record.setSleepHours(request.getSleepHours());
        record.setActivityLevel(request.getActivityLevel());
        record.setNotes(request.getNotes());
        record.setRecordedDate(request.getRecordedDate());
    }

    private BigDecimal calculateBmi(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal meters = height.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weight.divide(meters.multiply(meters), 2, RoundingMode.HALF_UP);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private HealthTrackingResponse toResponse(HealthTracking record) {
        User user = record.getUser();

        return HealthTrackingResponse.builder()
                .id(record.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .weight(record.getWeight())
                .height(record.getHeight())
                .bmi(record.getBmi())
                .bloodPressure(record.getBloodPressure())
                .sugarLevel(record.getSugarLevel())
                .waterIntake(record.getWaterIntake())
                .sleepHours(record.getSleepHours())
                .activityLevel(record.getActivityLevel())
                .notes(record.getNotes())
                .recordedDate(record.getRecordedDate())
                .createdAt(record.getCreatedAt())
                .age(user.getAge())
                .gender(user.getGender())
                .goal(user.getGoal())
                .build();
    }
}
