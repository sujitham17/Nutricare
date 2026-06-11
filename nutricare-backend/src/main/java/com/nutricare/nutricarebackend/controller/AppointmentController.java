package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.AppointmentBookingRequest;
import com.nutricare.nutricarebackend.dto.AppointmentCancelRequest;
import com.nutricare.nutricarebackend.dto.AppointmentRatingRequest;
import com.nutricare.nutricarebackend.dto.AppointmentResponse;
import com.nutricare.nutricarebackend.dto.AppointmentStatusUpdateRequest;
import com.nutricare.nutricarebackend.dto.ApiErrorResponse;
import com.nutricare.nutricarebackend.dto.ConsultationNotesRequest;
import com.nutricare.nutricarebackend.service.AppointmentService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(
            Authentication authentication,
            @Valid @RequestBody AppointmentBookingRequest request
    ) {
        try {
            logAuthenticatedUser("POST /api/appointments/book", authentication);
            log.info(
                    "POST /api/appointments/book request: userId={}, dieticianId={}, appointmentDate={}, appointmentTime={}, notes={}",
                    extractUserId(authentication),
                    request.getDieticianId(),
                    request.getAppointmentDate(),
                    request.getAppointmentTime(),
                    request.getNotes()
            );

            subscriptionService.requireFeature(authentication.getName(), "BOOK_APPOINTMENT");
            AppointmentResponse response = appointmentService.bookAppointment(authentication.getName(), request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
        } catch (ResponseStatusException e) {
            log.warn(
                    "Appointment booking validation error: status={}, reason={}",
                    e.getStatusCode(),
                    e.getReason()
            );
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiErrorResponse.builder()
                            .success(false)
                            .reason(e.getReason())
                            .build());
        } catch (Exception e) {
            log.error(
                    "Unexpected error booking appointment for user: {}, dietician: {}, date: {}, time: {}",
                    extractUserId(authentication),
                    request.getDieticianId(),
                    request.getAppointmentDate(),
                    request.getAppointmentTime(),
                    e
            );
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.of(
                            "Appointment booking failed: " + e.getMessage(),
                            "APPOINTMENT_BOOKING_ERROR"
                    ));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication authentication) {
        logAuthenticatedUser("GET /api/appointments/my", authentication);
        return ResponseEntity.ok(appointmentService.getMyAppointments(authentication.getName()));
    }

    @GetMapping("/my-approved-pending-payment")
    public ResponseEntity<List<AppointmentResponse>> getApprovedPendingPaymentAppointments(Authentication authentication) {
        logAuthenticatedUser("GET /api/appointments/my-approved-pending-payment", authentication);
        return ResponseEntity.ok(appointmentService.getApprovedPendingPaymentAppointments(authentication.getName()));
    }

    @GetMapping("/dietician")
    public ResponseEntity<List<AppointmentResponse>> getDieticianAppointments(Authentication authentication) {
        logAuthenticatedUser("GET /api/appointments/dietician", authentication);
        return ResponseEntity.ok(appointmentService.getDieticianAppointments(authentication.getName()));
    }

    @GetMapping("/dietician/pending")
    public ResponseEntity<List<AppointmentResponse>> getDieticianPendingAppointments(Authentication authentication) {
        logAuthenticatedUser("GET /api/appointments/dietician/pending", authentication);
        return ResponseEntity.ok(appointmentService.getDieticianPendingAppointments(authentication.getName()));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<AppointmentResponse> approveAppointment(
            Authentication authentication,
            @PathVariable Long id
    ) {
        logAuthenticatedUser("PUT /api/appointments/{id}/approve", authentication);
        return ResponseEntity.ok(appointmentService.approveAppointment(authentication.getName(), id));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request
    ) {
        logAuthenticatedUser("PUT /api/appointments/{id}/reschedule", authentication);
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(authentication.getName(), id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request
    ) {
        logAuthenticatedUser("PUT /api/appointments/{id}/status", authentication);
        return ResponseEntity.ok(appointmentService.updateStatus(authentication.getName(), id, request));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AppointmentCancelRequest request
    ) {
        logAuthenticatedUser("PUT /api/appointments/{id}/cancel", authentication);
        return ResponseEntity.ok(appointmentService.cancelAppointment(authentication.getName(), id, request));
    }

    @PostMapping("/{id}/meeting")
    public ResponseEntity<AppointmentResponse> generateMeetingLink(
            Authentication authentication,
            @PathVariable Long id
    ) {
        logAuthenticatedUser("POST /api/appointments/{id}/meeting", authentication);
        subscriptionService.requireFeature(authentication.getName(), "VIDEO_CALL");
        return ResponseEntity.ok(appointmentService.generateMeetingLink(authentication.getName(), id));
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<AppointmentResponse> rateDietician(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRatingRequest request
    ) {
        logAuthenticatedUser("PUT /api/appointments/{id}/rating", authentication);
        return ResponseEntity.ok(appointmentService.rateDietician(authentication.getName(), id, request));
    }

    @PutMapping("/{id}/consultation-notes")
    public ResponseEntity<AppointmentResponse> addConsultationNotes(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ConsultationNotesRequest request
    ) {
        logAuthenticatedUser("PUT /api/appointments/{id}/consultation-notes", authentication);
        return ResponseEntity.ok(appointmentService.addConsultationNotes(authentication.getName(), id, request));
    }

    private void logAuthenticatedUser(String endpoint, Authentication authentication) {
        log.info(
                "{} authenticated email={}, authorities={}",
                endpoint,
                authentication.getName(),
                authentication.getAuthorities()
        );
    }

    private String extractUserId(Authentication authentication) {
        try {
            return authentication.getName();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
