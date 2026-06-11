package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.AppointmentBookingRequest;
import com.nutricare.nutricarebackend.dto.AppointmentCancelRequest;
import com.nutricare.nutricarebackend.dto.AppointmentRatingRequest;
import com.nutricare.nutricarebackend.dto.AppointmentResponse;
import com.nutricare.nutricarebackend.dto.AppointmentStatusUpdateRequest;
import com.nutricare.nutricarebackend.dto.AvailableSlotResponse;
import com.nutricare.nutricarebackend.dto.ConsultationNotesRequest;
import com.nutricare.nutricarebackend.dto.RatingRequest;
import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.BookingStatus;
import com.nutricare.nutricarebackend.entity.DieticianProfile;
import com.nutricare.nutricarebackend.entity.MeetingStatus;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.RefundStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.DieticianProfileRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.nutricare.nutricarebackend.dto.UserSubscriptionResponse;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private static final String JITSI_MEET_BASE_URL = "https://meet.jit.si/NutriCare-Appointment-";
    private static final String LEGACY_GOOGLE_MEET_BASE_URL = "https://meet.google.com/";
    private static final String USER_REFUND_POPUP = "Your consultation fee will be refunded within 7 working days.";
    private static final String DIETICIAN_CANCEL_POPUP = "Appointment cancelled successfully. User and admin have been notified.";
    private static final String USER_REFUND_MESSAGE = "Your consultation fee will be refunded within 7 working days.";
    private static final String DIETICIAN_CANCELLED_USER_MESSAGE = "Your dietician cancelled the appointment. Your consultation fee will be refunded within 7 working days.";
    private static final String USER_CANCELLED_DIETICIAN_MESSAGE = "The user cancelled the appointment.";
    private static final String ADMIN_PAID_CANCELLED_MESSAGE_TEMPLATE = "A paid appointment was cancelled by %s. Refund is pending and should be processed within 7 working days.";
    private static final DateTimeFormatter SMS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter SMS_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter SLOT_LABEL_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final List<AppointmentStatus> OCCUPIED_STATUSES = List.of(
            AppointmentStatus.REQUESTED,
            AppointmentStatus.PENDING_APPROVAL,
            AppointmentStatus.PENDING,
            AppointmentStatus.RESCHEDULED,
            AppointmentStatus.APPROVED,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.IN_PROGRESS
    );

    private final AppointmentRepository appointmentRepository;
    private final DieticianProfileRepository dieticianProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RatingService ratingService;
    private final AuditLogService auditLogService;
    private final SubscriptionService subscriptionService;

    public AppointmentResponse bookAppointment(String email, AppointmentBookingRequest request) {
        try {
            log.info("=== APPOINTMENT BOOKING START ===");
            log.info("Email: {}", email);
            log.info("Request: dieticianId={}, appointmentDate={}, appointmentTime={}",
                    request.getDieticianId(), request.getAppointmentDate(), request.getAppointmentTime());

            // 1. dieticianId is not null
            if (request.getDieticianId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dieticianId is required");
            }

            // 2. appointmentDate is not null
            if (request.getAppointmentDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointmentDate is required");
            }

            // 3. appointmentTime is not null
            if (request.getAppointmentTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointmentTime is required");
            }

            // 4. logged-in user exists
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logged-in user does not exist");
            }
            log.info("Authenticated user: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

            // Validate user role
            if (user.getRole() != Role.USER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only users can book appointments. Your role: " + user.getRole());
            }

            // 5. dietician exists
            User dietician = userRepository.findById(request.getDieticianId()).orElse(null);
            if (dietician == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dietician does not exist");
            }
            log.info("Selected dietician: id={}, email={}, role={}", dietician.getId(), dietician.getEmail(), dietician.getRole());

            if (dietician.getRole() != Role.DIETICIAN) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selected user is not a dietician. Actual role: " + dietician.getRole()
                );
            }

            // Check dietician appointment limit
            UserSubscriptionResponse dieticianSub = subscriptionService.getCurrentSubscription(dietician);
            if (dieticianSub.getStatus() != SubscriptionStatus.ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "This dietician has reached their appointment limit. Please choose another dietician."
                );
            }

            Integer maxAppointments = dieticianSub.getMaxAppointments();
            if (maxAppointments != null && maxAppointments != -1) {
                List<AppointmentStatus> limitStatuses = List.of(
                        AppointmentStatus.PENDING_APPROVAL,
                        AppointmentStatus.APPROVED,
                        AppointmentStatus.CONFIRMED,
                        AppointmentStatus.RESCHEDULED
                );
                long activeCount = appointmentRepository.countByDieticianAndStatusIn(dietician, limitStatuses);
                if (activeCount >= maxAppointments) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "This dietician has reached their appointment limit. Please choose another dietician."
                    );
                }
            }

            // 6. dietician availability exists
            DieticianProfile profile = ensureDieticianAvailability(dietician);

            LocalTime availableFrom = profile.getAvailableFrom();
            LocalTime availableTo = profile.getAvailableTo();
            LocalTime requestedTime = request.getAppointmentTime();

            // 7. requested time is inside availableFrom and availableTo
            if (requestedTime.isBefore(availableFrom) || !requestedTime.isBefore(availableTo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested time is outside dietician availability");
            }
            if (!isAvailableDay(profile, request.getAppointmentDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected day is outside dietician availability");
            }

            // 8. slot is not already occupied
            boolean slotConflict = isSlotOccupied(dietician, request.getAppointmentDate(), requestedTime);

            // Add logs: userId, dieticianId, appointmentDate, appointmentTime, availableFrom, availableTo, slotConflict true/false
            log.info("Appointment booking validation: userId={}, dieticianId={}, appointmentDate={}, appointmentTime={}, availableFrom={}, availableTo={}, slotConflict={}",
                    user.getId(), dietician.getId(), request.getAppointmentDate(), requestedTime, availableFrom, availableTo, slotConflict);

            if (slotConflict) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This slot is already occupied");
            }

            // 9. appointment status enum value exists
            AppointmentStatus appointmentStatus;
            try {
                appointmentStatus = AppointmentStatus.valueOf("PENDING_APPROVAL");
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AppointmentStatus value PENDING_APPROVAL does not exist");
            }

            // 10. payment status enum value exists
            PaymentStatus paymentStatus;
            try {
                paymentStatus = PaymentStatus.valueOf("PENDING");
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PaymentStatus value PENDING does not exist");
            }

            BookingStatus bookingStatus;
            try {
                bookingStatus = BookingStatus.valueOf("PENDING_APPROVAL");
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BookingStatus value PENDING_APPROVAL does not exist");
            }

            // Get consultation fee
            java.math.BigDecimal consultationFee = dietician.getConsultationFee();
            log.info("Consultation fee: {}", consultationFee);
            if (consultationFee == null) {
                log.warn("Warning: Dietician has no consultation fee set");
                consultationFee = java.math.BigDecimal.ZERO;
            }

            // Create and save appointment
            Appointment appointment = Appointment.builder()
                    .user(user)
                    .dietician(dietician)
                    .appointmentDate(request.getAppointmentDate())
                    .appointmentTime(request.getAppointmentTime())
                    .status(appointmentStatus)
                    .bookingStatus(bookingStatus)
                    .consultationFee(consultationFee)
                    .paymentStatus(paymentStatus)
                    .refundStatus(RefundStatus.NOT_REQUIRED)
                    .notes(request.getNotes())
                    .build();

            log.info("Appointment entity created, attempting to save");
            Appointment savedAppointment = appointmentRepository.save(appointment);
            log.info("Appointment saved successfully: id={}", savedAppointment.getId());

            // Record audit log
            auditLogService.record(
                    user.getId(),
                    user.getRole(),
                    "APPOINTMENT_PAYMENT_PENDING",
                    "APPOINTMENTS",
                    "User " + user.getId() + " requested appointment " + savedAppointment.getId()
                            + " with dietician " + dietician.getId()
            );
            log.info("Audit log recorded");

            // Send notifications
            try {
                notifyAppointmentBooked(savedAppointment);
                log.info("Notifications sent");
            } catch (Exception notificationError) {
                log.warn("Failed to send appointment notification, but appointment was created", notificationError);
            }

            log.info("=== APPOINTMENT BOOKING SUCCESS ===");
            return toResponse(savedAppointment);
        } catch (ResponseStatusException e) {
            log.warn("=== APPOINTMENT BOOKING VALIDATION FAILED ===");
            log.warn("Status: {}, Reason: {}", e.getStatusCode(), e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("=== APPOINTMENT BOOKING UNEXPECTED ERROR ===");
            log.error("User: {}, Dietician: {}, Date: {}, Time: {}",
                    email, request.getDieticianId(), request.getAppointmentDate(), request.getAppointmentTime());
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Full stack trace:", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to book appointment: " + e.getClass().getSimpleName() + " - " + e.getMessage()
            );
        }
    }

    public List<AppointmentResponse> getMyAppointments(String email) {
        User user = getAuthenticatedUser(email);
        logAuthenticatedUser(user);
        requireRole(user, Role.USER, "Only users can view their appointments");

        return appointmentRepository.findByUserOrderByAppointmentDateDescAppointmentTimeDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentResponse> getApprovedPendingPaymentAppointments(String email) {
        User user = getAuthenticatedUser(email);
        requireRole(user, Role.USER, "Only users can view their approved pending payment appointments");

        return appointmentRepository.findByUserAndBookingStatusAndPaymentStatusNot(
                user,
                BookingStatus.APPROVED,
                PaymentStatus.SUCCESS
        )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentResponse> getDieticianAppointments(String email) {
        User dietician = getAuthenticatedUser(email);
        logAuthenticatedUser(dietician);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can view dietician appointments");

        List<BookingStatus> statuses = List.of(
                BookingStatus.PENDING_APPROVAL,
                BookingStatus.APPROVED,
                BookingStatus.RESCHEDULED,
                BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED
        );
        return appointmentRepository.findByDieticianAndBookingStatusInOrderByAppointmentDateDescAppointmentTimeDesc(dietician, statuses)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentResponse> getDieticianPendingAppointments(String email) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can view pending appointments");

        return appointmentRepository.findByDieticianAndBookingStatusOrderByAppointmentDateDescAppointmentTimeDesc(
                dietician,
                BookingStatus.PENDING_APPROVAL
        )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentResponse approveAppointment(String email, Long appointmentId) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can approve appointments");

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getDietician().getId().equals(dietician.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot approve another dietician's appointment");
        }

        if (appointment.getBookingStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending approval appointments can be approved");
        }

        appointment.setBookingStatus(BookingStatus.APPROVED);
        appointment.setStatus(AppointmentStatus.APPROVED);
        notifyAppointmentApproved(appointment);

        Appointment saved = appointmentRepository.save(appointment);

        auditLogService.record(
                dietician.getId(),
                dietician.getRole(),
                "APPOINTMENT_APPROVED",
                "APPOINTMENTS",
                "Dietician " + dietician.getId() + " approved appointment " + saved.getId()
        );

        return toResponse(saved);
    }

    public AppointmentResponse rescheduleAppointment(String email, Long appointmentId, AppointmentStatusUpdateRequest request) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can reschedule appointments");

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getDietician().getId().equals(dietician.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot reschedule another dietician's appointment");
        }

        if (request.getAppointmentDate() == null || request.getAppointmentTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointmentDate and appointmentTime are required to reschedule");
        }

        validateSlotAvailable(dietician, request.getAppointmentDate(), request.getAppointmentTime());

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setBookingStatus(BookingStatus.RESCHEDULED);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointment.setPaymentStatus(PaymentStatus.PENDING); // Reset payment status to pending on reschedule

        notifyAppointmentRescheduled(appointment);

        Appointment saved = appointmentRepository.save(appointment);

        auditLogService.record(
                dietician.getId(),
                dietician.getRole(),
                "APPOINTMENT_RESCHEDULED",
                "APPOINTMENTS",
                "Dietician " + dietician.getId() + " rescheduled appointment " + saved.getId()
        );

        return toResponse(saved);
    }

    public AppointmentResponse updateStatus(String email, Long appointmentId, AppointmentStatusUpdateRequest request) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can update appointment status");

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getDietician().getId().equals(dietician.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another dietician's appointment");
        }

        if (request.getStatus() == AppointmentStatus.PENDING || request.getStatus() == AppointmentStatus.REQUESTED || request.getStatus() == AppointmentStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status can only be APPROVED, RESCHEDULED, IN_PROGRESS, COMPLETED, or CANCELLED");
        }
        if (request.getStatus() == AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is confirmed automatically after successful payment");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed appointments cannot be updated");
        }
        if (request.getStatus() == AppointmentStatus.IN_PROGRESS && appointment.getMeetingLink() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Generate a meeting link before starting consultation");
        }

        if (request.getStatus() == AppointmentStatus.RESCHEDULED) {
            if (request.getAppointmentDate() == null || request.getAppointmentTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointmentDate and appointmentTime are required to reschedule");
            }
            validateSlotAvailable(dietician, request.getAppointmentDate(), request.getAppointmentTime());
            appointment.setAppointmentDate(request.getAppointmentDate());
            appointment.setAppointmentTime(request.getAppointmentTime());
            appointment.setBookingStatus(BookingStatus.RESCHEDULED);
            appointment.setPaymentStatus(PaymentStatus.PENDING);
            appointment.setStatus(AppointmentStatus.RESCHEDULED);
            notifyAppointmentRescheduled(appointment);
        } else {
            appointment.setStatus(request.getStatus());
        }
        if (request.getStatus() == AppointmentStatus.APPROVED) {
            appointment.setBookingStatus(BookingStatus.APPROVED);
            notifyAppointmentApproved(appointment);
        }
        if (request.getStatus() == AppointmentStatus.CANCELLED) {
            applyCancellation(appointment, dietician, request.getReason());
            notifyCancellation(appointment, dietician, request.getReason());
        }
        if (request.getStatus() == AppointmentStatus.IN_PROGRESS) {
            appointment.setMeetingStatus(MeetingStatus.IN_PROGRESS);
        } else if (request.getStatus() == AppointmentStatus.COMPLETED) {
            appointment.setMeetingStatus(MeetingStatus.ENDED);
        } else if (request.getStatus() == AppointmentStatus.CANCELLED) {
            appointment.setMeetingStatus(MeetingStatus.ENDED);
        }
        Appointment saved = appointmentRepository.save(appointment);
        if (saved.getStatus() == AppointmentStatus.APPROVED || saved.getStatus() == AppointmentStatus.CONFIRMED) {
            auditLogService.record(
                    dietician.getId(),
                    dietician.getRole(),
                    "APPOINTMENT_CONFIRMED",
                    "APPOINTMENTS",
                    "Dietician " + dietician.getId() + " confirmed appointment " + saved.getId()
            );
        } else if (saved.getStatus() == AppointmentStatus.CANCELLED) {
            auditLogService.record(
                    dietician.getId(),
                    dietician.getRole(),
                    "APPOINTMENT_CANCELLED",
                    "APPOINTMENTS",
                    "Dietician " + dietician.getId() + " cancelled appointment " + saved.getId()
            );
        } else if (saved.getStatus() == AppointmentStatus.COMPLETED) {
            auditLogService.record(
                    dietician.getId(),
                    dietician.getRole(),
                    "APPOINTMENT_COMPLETED",
                    "APPOINTMENTS",
                    "Dietician " + dietician.getId() + " completed appointment " + saved.getId()
            );
        }
        return toResponse(saved);
    }

    public List<AvailableSlotResponse> getAvailableSlots(Long dieticianId, LocalDate date) {
        User dietician = userRepository.findById(dieticianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dietician not found"));
        if (dietician.getRole() != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a dietician");
        }
        DieticianProfile profile = ensureDieticianAvailability(dietician);
        LocalTime from = profile.getAvailableFrom();
        LocalTime to = profile.getAvailableTo();
        if (!isAvailableDay(profile, date) || !from.isBefore(to)) {
            return List.of();
        }

        java.util.ArrayList<AvailableSlotResponse> slots = new java.util.ArrayList<>();
        for (LocalTime slot = from; slot.isBefore(to); slot = slot.plusMinutes(30)) {
            if (!isSlotOccupied(dietician, date, slot)) {
                slots.add(AvailableSlotResponse.builder()
                        .label(slot.format(SLOT_LABEL_FORMAT))
                        .value(slot)
                        .build());
            }
        }
        return slots;
    }

    public AppointmentResponse generateMeetingLink(String email, Long appointmentId) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can generate consultation meeting links");

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appointment.getDietician().getId().equals(dietician.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another dietician's appointment");
        }
        if (appointment.getStatus() != AppointmentStatus.APPROVED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting link can only be generated for confirmed appointments");
        }

        if (appointment.getMeetingLink() == null || appointment.getMeetingLink().isBlank() || isLegacyGoogleMeetLink(appointment.getMeetingLink())) {
            appointment.setMeetingLink(generateJitsiMeetLink(appointment.getId()));
            appointment.setMeetingStatus(MeetingStatus.SCHEDULED);
            appointment.setMeetingCreatedAt(LocalDateTime.now());
            Appointment saved = appointmentRepository.save(appointment);
            notifyMeetingScheduled(saved);
            auditLogService.record(
                    dietician.getId(),
                    dietician.getRole(),
                    "MEETING_GENERATED",
                    "APPOINTMENTS",
                    "Dietician " + dietician.getId() + " generated meeting link for appointment " + saved.getId()
            );
            return toResponse(saved);
        }

        return toResponse(appointment);
    }

    public AppointmentResponse rateDietician(String email, Long appointmentId, AppointmentRatingRequest request) {
        User user = getAuthenticatedUser(email);
        requireRole(user, Role.USER, "Only users can rate dieticians");

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot rate another user's appointment");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dietician can be rated after completed consultation");
        }

        RatingRequest ratingRequest = new RatingRequest();
        ratingRequest.setAppointmentId(appointmentId);
        ratingRequest.setRating(request.getRating());
        ratingRequest.setReview(request.getComment());
        ratingService.createRating(email, ratingRequest);
        return toResponse(appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found")));
    }

    public AppointmentResponse addConsultationNotes(String email, Long appointmentId, ConsultationNotesRequest request) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can add consultation notes");

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appointment.getDietician().getId().equals(dietician.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another dietician's appointment");
        }
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consultation notes can be added after consultation starts");
        }

        appointment.setConsultationNotes(request.getConsultationNotes());
        return toResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancelAppointment(String email, Long appointmentId, AppointmentCancelRequest request) {
        User requester = getAuthenticatedUser(email);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        boolean ownsAppointment = appointment.getUser().getId().equals(requester.getId());
        boolean ownsDieticianSchedule = appointment.getDietician().getId().equals(requester.getId());
        if (!ownsAppointment && !ownsDieticianSchedule && requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot cancel another account's appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed appointments cannot be cancelled");
        }

        String reason = request == null ? null : request.getReason();
        applyCancellation(appointment, requester, reason);
        if (reason != null && !reason.isBlank()) {
            String existingNotes = appointment.getNotes() == null ? "" : appointment.getNotes() + "\n";
            appointment.setNotes(existingNotes + "Cancellation reason: " + reason.trim());
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        if (appointment.getUser().getRole() == Role.USER
                && !appointmentRepository.existsByUserAndStatusNot(appointment.getUser(), AppointmentStatus.CANCELLED)) {
            User user = appointment.getUser();
            user.setAppointmentCompleted(false);
            user.setOnboardingCompleted(false);
            userRepository.save(user);
        }
        if (appointment.getDietician().getRole() == Role.DIETICIAN
                && !appointmentRepository.existsByDieticianAndStatusNot(appointment.getDietician(), AppointmentStatus.CANCELLED)) {
            User dietician = appointment.getDietician();
            dietician.setAppointmentCompleted(false);
            dietician.setOnboardingCompleted(false);
            userRepository.save(dietician);
        }
        notifyCancellation(savedAppointment, requester, reason);
        auditLogService.record(
                requester.getId(),
                requester.getRole(),
                "APPOINTMENT_CANCELLED",
                "APPOINTMENTS",
                "Actor " + requester.getId() + " cancelled appointment " + savedAppointment.getId()
        );

        return toResponse(savedAppointment);
    }

    private void notifyMeetingScheduled(Appointment appointment) {
        notificationService.sendNotification(
                appointment.getUser(),
                appointment.getDietician(),
                "Video Consultation Ready",
                "Your NutriCare video consultation link is ready: " + appointment.getMeetingLink(),
                "MEETING_LINK_CREATED"
        );
    }

    private void notifyAppointmentBooked(Appointment appointment) {
        User user = appointment.getUser();
        User dietician = appointment.getDietician();
        String date = appointment.getAppointmentDate().format(SMS_DATE_FORMAT);
        String time = appointment.getAppointmentTime().format(SMS_TIME_FORMAT);

        notificationService.sendInAppNotification(
                dietician,
                user,
                "New Appointment Request",
                "You have received a new appointment request from " + user.getFullName() + " on " + date + " at " + time + ".",
                "APPOINTMENT_BOOKED"
        );

        try {
            String smsMessage = "NutriCare: New appointment request from " + user.getFullName() + " on " + date + " at " + time + ". Please approve, reschedule, or cancel.";
            notificationService.sendSms(dietician, "New Appointment Request", smsMessage);
        } catch (Exception e) {
            log.error("Failed to send booking SMS to dietician: {}", e.getMessage());
        }
    }

    private void notifyAppointmentApproved(Appointment appointment) {
        String date = appointment.getAppointmentDate().format(SMS_DATE_FORMAT);
        String time = appointment.getAppointmentTime().format(SMS_TIME_FORMAT);

        notificationService.sendInAppNotification(
                appointment.getUser(),
                appointment.getDietician(),
                "Appointment Approved",
                "Dietician approved your appointment. Please pay the consultation fee to confirm.",
                "APPOINTMENT_APPROVED"
        );

        try {
            String smsMessage = "NutriCare: Dietician " + appointment.getDietician().getFullName() + " approved your appointment on " + date + " at " + time + ". Please pay the consultation fee to confirm.";
            notificationService.sendSms(appointment.getUser(), "Appointment Approved", smsMessage);
        } catch (Exception e) {
            log.error("Failed to send approval SMS to user: {}", e.getMessage());
        }
    }

    private void notifyAppointmentRescheduled(Appointment appointment) {
        notificationService.sendNotification(
                appointment.getUser(),
                appointment.getDietician(),
                "Appointment Rescheduled",
                "Dietician has rescheduled your appointment. Please review and pay consultation fee if accepted.",
                "APPOINTMENT_RESCHEDULED"
        );
    }

    private void notifyAppointmentRejected(Appointment appointment, String reason) {
        String rejectionReason = reason == null || reason.isBlank() ? "No reason provided" : reason.trim();

        notificationService.sendNotification(
                appointment.getUser(),
                appointment.getDietician(),
                "Appointment Cancelled",
                "Your appointment has been cancelled. Reason: " + rejectionReason,
                "APPOINTMENT_CANCELLED"
        );
    }

    private void applyCancellation(Appointment appointment, User requester, String reason) {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setBookingStatus(BookingStatus.CANCELLED);
        appointment.setMeetingStatus(MeetingStatus.ENDED);
        appointment.setCancelledBy(requester.getId());
        appointment.setCancellationReason(reason == null ? null : reason.trim());
        appointment.setCancelledAt(LocalDateTime.now());
        if (appointment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            appointment.setRefundStatus(RefundStatus.PENDING);
            appointment.setRefundExpectedBy(addWorkingDays(java.time.LocalDate.now(), 7));
        } else {
            appointment.setRefundStatus(RefundStatus.NOT_REQUIRED);
            appointment.setRefundExpectedBy(null);
        }
    }

    private void notifyCancellation(Appointment appointment, User requester, String reason) {
        boolean paid = appointment.getPaymentStatus() == PaymentStatus.SUCCESS;
        String date = appointment.getAppointmentDate().format(SMS_DATE_FORMAT);
        String time = appointment.getAppointmentTime().format(SMS_TIME_FORMAT);

        if (requester.getRole() == Role.USER) {
            notificationService.sendInAppNotification(
                    appointment.getDietician(),
                    requester,
                    "Appointment Cancelled",
                    "Your appointment has been cancelled. Reason: " + (reason == null || reason.isBlank() ? "No reason provided" : reason.trim()),
                    "APPOINTMENT_CANCELLED"
            );
            try {
                String smsMessage = "NutriCare: " + appointment.getUser().getFullName() + " cancelled the appointment on " + date + " at " + time + ".";
                notificationService.sendSms(appointment.getDietician(), "Appointment Cancelled", smsMessage);
            } catch (Exception e) {
                log.error("Failed to send cancellation SMS to dietician: {}", e.getMessage());
            }
        } else if (requester.getRole() == Role.DIETICIAN) {
            notificationService.sendInAppNotification(
                appointment.getUser(),
                requester,
                "Appointment Cancelled",
                "Dietician cancelled this appointment. Please book another dietician.",
                "APPOINTMENT_CANCELLED"
            );
            try {
                String smsMessage = "NutriCare: Dietician " + appointment.getDietician().getFullName() + " cancelled the appointment. Please book another dietician.";
                notificationService.sendSms(appointment.getUser(), "Appointment Cancelled", smsMessage);
            } catch (Exception e) {
                log.error("Failed to send cancellation SMS to user: {}", e.getMessage());
            }
        } else if (requester.getRole() == Role.ADMIN) {
            notificationService.sendInAppNotification(
                    appointment.getUser(),
                    requester,
                    "Appointment Cancelled",
                    "Your appointment has been cancelled. Reason: " + (reason == null || reason.isBlank() ? "No reason provided" : reason.trim()),
                    "APPOINTMENT_CANCELLED"
            );
        }

        if (paid) {
            notifyUserRefundPending(appointment, requester);
            notifyAdminsPaidAppointmentCancelled(requester);
        }
    }

    private void notifyUserRefundPending(Appointment appointment, User requester) {
        String message = requester.getRole() == Role.DIETICIAN
                ? DIETICIAN_CANCELLED_USER_MESSAGE
                : USER_REFUND_MESSAGE;
        notificationService.sendNotification(
                appointment.getUser(),
                requester,
                "Refund Pending",
                message,
                "REFUND_PENDING"
        );
    }

    private void notifyAdminsPaidAppointmentCancelled(User requester) {
        String message = String.format(ADMIN_PAID_CANCELLED_MESSAGE_TEMPLATE, requester.getRole().name());
        userRepository.findByRole(Role.ADMIN)
                .stream()
                .filter(admin -> admin.getStatus() == com.nutricare.nutricarebackend.entity.UserStatus.ACTIVE)
                .forEach(admin -> notificationService.sendNotification(
                        admin,
                        requester,
                        "Paid Appointment Cancelled",
                        message,
                        "REFUND_PENDING"
                ));
    }

    private String generateJitsiMeetLink(Long appointmentId) {
        return JITSI_MEET_BASE_URL + appointmentId;
    }

    private boolean isLegacyGoogleMeetLink(String meetingLink) {
        return meetingLink != null && meetingLink.toLowerCase().startsWith(LEGACY_GOOGLE_MEET_BASE_URL);
    }

    private User getAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateSlotAvailable(User dietician, LocalDate date, LocalTime time) {
        DieticianProfile profile = ensureDieticianAvailability(dietician);

        LocalTime from = profile.getAvailableFrom();
        LocalTime to = profile.getAvailableTo();
        boolean slotConflict = isSlotOccupied(dietician, date, time);
        log.info("Validating slot: dieticianId={}, appointmentDate={}, appointmentTime={}, availableFrom={}, availableTo={}, slotConflict={}",
                dietician.getId(), date, time, from, to, slotConflict);

        if (!isAvailableDay(profile, date) || time.isBefore(from) || !time.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected slot is outside dietician availability");
        }
        if (slotConflict) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This slot is already occupied. Please choose another time.");
        }
    }

    private boolean isSlotOccupied(User dietician, LocalDate date, LocalTime time) {
        return appointmentRepository.existsByDieticianAndAppointmentDateAndAppointmentTimeAndStatusIn(
                dietician,
                date,
                time,
                OCCUPIED_STATUSES
        );
    }

    private DieticianProfile ensureDieticianAvailability(User dietician) {
        DieticianProfile profile = dieticianProfileRepository.findByUser(dietician).orElse(null);
        if (profile == null) {
            log.info("Creating default profile with default availability for dietician: {}", dietician.getEmail());
            profile = DieticianProfile.builder()
                    .user(dietician)
                    .specialization(dietician.getSpecialization() != null ? dietician.getSpecialization() : "General Wellness")
                    .experience(dietician.getExperience() != null ? dietician.getExperience() : 0)
                    .qualification(dietician.getDegree() != null ? dietician.getDegree() : "BSc Nutrition")
                    .consultationFee(dietician.getConsultationFee() != null ? dietician.getConsultationFee() : java.math.BigDecimal.ZERO)
                    .bio(dietician.getBio())
                    .profileImage(dietician.getProfileImage())
                    .availableFrom(LocalTime.of(9, 0))
                    .availableTo(LocalTime.of(18, 0))
                    .availableDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                    .build();
            profile = dieticianProfileRepository.save(profile);
        } else if (profile.getAvailableFrom() == null || profile.getAvailableTo() == null || profile.getAvailableDays() == null || profile.getAvailableDays().isBlank()) {
            log.info("Setting default availability for dietician: {}", dietician.getEmail());
            profile.setAvailableFrom(LocalTime.of(9, 0));
            profile.setAvailableTo(LocalTime.of(18, 0));
            profile.setAvailableDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
            profile = dieticianProfileRepository.save(profile);
        }
        return profile;
    }

    private boolean isAvailableDay(DieticianProfile profile, LocalDate date) {
        if (profile == null || profile.getAvailableDays() == null || profile.getAvailableDays().isBlank()) {
            return true;
        }
        String day = date.getDayOfWeek().name();
        return java.util.Arrays.stream(profile.getAvailableDays().split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(value -> value.equals(day) || value.equals(day.substring(0, 3)));
    }

    private void requireRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void logAuthenticatedUser(User user) {
        log.info("Authenticated email: {}", user.getEmail());
        log.info("Authenticated role: {}", user.getRole());
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        User user = appointment.getUser();
        User dietician = appointment.getDietician();

        return AppointmentResponse.builder()
                .success(appointment.getStatus() == AppointmentStatus.CANCELLED ? Boolean.TRUE : null)
                .id(appointment.getId())
                .appointmentId(appointment.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .userProfileImage(user.getProfileImage())
                .userProfileImageUrl(user.getProfileImage())
                .dieticianId(dietician.getId())
                .dieticianFullName(dietician.getFullName())
                .dieticianEmail(dietician.getEmail())
                .dieticianProfileImage(dietician.getProfileImage())
                .dieticianProfileImageUrl(dietician.getProfileImage())
                .dieticianAverageRating(ratingService.averageRating(dietician))
                .dieticianTotalRatings(ratingService.totalRatings(dietician))
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .bookingStatus(appointment.getBookingStatus())
                .consultationFee(appointment.getConsultationFee())
                .paymentStatus(appointment.getPaymentStatus())
                .paymentId(appointment.getPaymentId())
                .orderId(appointment.getOrderId())
                .paidAt(appointment.getPaidAt())
                .refundStatus(appointment.getRefundStatus())
                .refundExpectedBy(appointment.getRefundExpectedBy())
                .cancelledBy(appointment.getCancelledBy() == null ? null : cancelledByRole(appointment))
                .cancelledById(appointment.getCancelledBy())
                .cancelledByRole(appointment.getCancelledBy() == null ? null : cancelledByRole(appointment))
                .cancellationReason(appointment.getCancellationReason())
                .cancelledAt(appointment.getCancelledAt())
                .popupMessage(cancelPopupMessage(appointment))
                .notes(appointment.getNotes())
                .meetingLink(appointment.getMeetingLink())
                .meetingStatus(appointment.getMeetingStatus())
                .meetingCreatedAt(appointment.getMeetingCreatedAt())
                .userRating(appointment.getUserRating())
                .userRatingComment(appointment.getUserRatingComment())
                .consultationNotes(appointment.getConsultationNotes())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    private java.time.LocalDate addWorkingDays(java.time.LocalDate startDate, int workingDays) {
        java.time.LocalDate date = startDate;
        int added = 0;
        while (added < workingDays) {
            date = date.plusDays(1);
            java.time.DayOfWeek day = date.getDayOfWeek();
            if (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return date;
    }

    private String cancelledByRole(Appointment appointment) {
        Long actorId = appointment.getCancelledBy();
        if (actorId == null) {
            return null;
        }
        if (appointment.getUser().getId().equals(actorId)) {
            return Role.USER.name();
        }
        if (appointment.getDietician().getId().equals(actorId)) {
            return Role.DIETICIAN.name();
        }
        return Role.ADMIN.name();
    }

    private String cancelPopupMessage(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.CANCELLED || appointment.getRefundStatus() != RefundStatus.PENDING) {
            return null;
        }
        String cancelledByRole = cancelledByRole(appointment);
        if (Role.DIETICIAN.name().equals(cancelledByRole)) {
            return DIETICIAN_CANCEL_POPUP;
        }
        if (Role.USER.name().equals(cancelledByRole)) {
            return USER_REFUND_POPUP;
        }
        return "Appointment cancelled successfully.";
    }
}
