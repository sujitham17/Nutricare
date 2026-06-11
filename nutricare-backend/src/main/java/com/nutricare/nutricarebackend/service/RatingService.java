package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.RatingRequest;
import com.nutricare.nutricarebackend.dto.RatingResponse;
import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.Rating;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.RatingRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final RatingRepository ratingRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public RatingResponse createRating(String email, RatingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only users can rate dieticians");
        }

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot rate another user's appointment");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dietician can be rated after completed consultation");
        }
        if (ratingRepository.existsByAppointment(appointment)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment is already rated");
        }

        Rating rating = Rating.builder()
                .appointment(appointment)
                .user(user)
                .dietician(appointment.getDietician())
                .rating(request.getRating())
                .review(request.getReview())
                .build();

        appointment.setUserRating(request.getRating());
        appointment.setUserRatingComment(request.getReview());
        appointmentRepository.save(appointment);

        Rating savedRating = ratingRepository.save(rating);
        notificationService.sendNotification(
                savedRating.getDietician(),
                user,
                "New Rating Received",
                "You received a new rating: " + savedRating.getRating() + "/5.",
                "USER_RATING_SUBMITTED"
        );
        auditLogService.record(
                user.getId(),
                user.getRole(),
                "RATING_SUBMITTED",
                "RATINGS",
                "User " + user.getId() + " submitted rating " + savedRating.getId()
                        + " for dietician " + savedRating.getDietician().getId()
        );

        return toResponse(savedRating);
    }

    public List<RatingResponse> getDieticianRatings(Long dieticianId) {
        User dietician = userRepository.findById(dieticianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dietician not found"));
        if (dietician.getRole() != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a dietician");
        }

        return ratingRepository.findByDieticianOrderByCreatedAtDesc(dietician)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RatingResponse> getMyRatings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.USER) {
            return ratingRepository.findByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (user.getRole() == Role.DIETICIAN) {
            return ratingRepository.findByDieticianOrderByCreatedAtDesc(user)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return ratingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Double averageRating(User dietician) {
        Double average = ratingRepository.averageByDietician(dietician);
        return average == null ? null : Math.round(average * 10.0) / 10.0;
    }

    public long totalRatings(User dietician) {
        return ratingRepository.countByDietician(dietician);
    }

    private RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .appointmentId(rating.getAppointment().getId())
                .userId(rating.getUser().getId())
                .dieticianId(rating.getDietician().getId())
                .rating(rating.getRating())
                .review(rating.getReview())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
