package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AppointmentRepository appointmentRepository;

    public void requireRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    public void requireAssignedDietician(User dietician, User user) {
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can access assigned user data");
        requireRole(user, Role.USER, "Requested account is not a user");

        if (!appointmentRepository.existsByUserAndDieticianAndStatusIn(
                user,
                dietician,
                java.util.List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.APPROVED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dietician is not assigned to this user");
        }
    }

    public boolean isAssignedDietician(User dietician, User user) {
        return dietician.getRole() == Role.DIETICIAN
                && user.getRole() == Role.USER
                && appointmentRepository.existsByUserAndDieticianAndStatusIn(
                        user,
                        dietician,
                        java.util.List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.APPROVED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)
                );
    }
}
