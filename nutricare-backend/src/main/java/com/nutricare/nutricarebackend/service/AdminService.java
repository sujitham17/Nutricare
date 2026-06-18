package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.AdminDashboardSummaryResponse;
import com.nutricare.nutricarebackend.dto.AdminActivityMetricResponse;
import com.nutricare.nutricarebackend.dto.AdminClientProgramResponse;
import com.nutricare.nutricarebackend.dto.AdminCreateDieticianRequest;
import com.nutricare.nutricarebackend.dto.AdminCreateUserRequest;
import com.nutricare.nutricarebackend.dto.AdminPaymentResponse;
import com.nutricare.nutricarebackend.dto.AdminRevenueResponse;
import com.nutricare.nutricarebackend.dto.AdminSubscriptionResponse;
import com.nutricare.nutricarebackend.dto.AdminUserActionRequest;
import com.nutricare.nutricarebackend.dto.AdminUserResponse;
import com.nutricare.nutricarebackend.dto.AppointmentResponse;
import com.nutricare.nutricarebackend.dto.NotificationLogResponse;
import com.nutricare.nutricarebackend.dto.SystemStatsResponse;
import com.nutricare.nutricarebackend.dto.DiseaseRequest;
import com.nutricare.nutricarebackend.dto.DiseaseResponse;
import com.nutricare.nutricarebackend.repository.DiseaseRepository;
import com.nutricare.nutricarebackend.service.DiseaseService;
import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.AdminActionLog;
import com.nutricare.nutricarebackend.entity.ConsultationPayment;
import com.nutricare.nutricarebackend.entity.DieticianProfile;
import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.MealComplianceStatus;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionPlan;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.SubscriptionTransaction;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.mongo.document.AuditLogDocument;
import com.nutricare.nutricarebackend.mongo.document.NotificationLogDocument;
import com.nutricare.nutricarebackend.mongo.document.ReportSnapshotDocument;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.AdminActionLogRepository;
import com.nutricare.nutricarebackend.repository.ConsultationPaymentRepository;
import com.nutricare.nutricarebackend.repository.DieticianProfileRepository;
import com.nutricare.nutricarebackend.repository.DietPlanRepository;
import com.nutricare.nutricarebackend.repository.MealComplianceRepository;
import com.nutricare.nutricarebackend.repository.SubscriptionTransactionRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int RECENT_LIMIT = 8;

    private final UserRepository userRepository;
    private final DieticianProfileRepository dieticianProfileRepository;
    private final DietPlanRepository dietPlanRepository;
    private final MealComplianceRepository mealComplianceRepository;
    private final AppointmentRepository appointmentRepository;
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;
    private final ConsultationPaymentRepository consultationPaymentRepository;
    private final SubscriptionService subscriptionService;
    private final RatingService ratingService;
    private final MongoNotificationLogService mongoNotificationLogService;
    private final MongoReportSnapshotService mongoReportSnapshotService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final AdminActionLogRepository adminActionLogRepository;
    private final DiseaseRepository diseaseRepository;
    private final DiseaseService diseaseService;

    public AdminDashboardSummaryResponse getDashboardSummary() {
        AdminRevenueResponse revenue = getRevenue();
        long followedMealCompliance = mealComplianceRepository.countByStatus(MealComplianceStatus.FOLLOWED);
        long notFollowedMealCompliance = mealComplianceRepository.countByStatus(MealComplianceStatus.NOT_FOLLOWED);
        long pendingMealCompliance = mealComplianceRepository.countByStatus(MealComplianceStatus.PENDING);
        long totalMealCompliance = followedMealCompliance + notFollowedMealCompliance + pendingMealCompliance;
        long totalAppointments = appointmentRepository.count();
        long completedAppointments = appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);
        long dueReviews = dietPlanRepository.findAll().stream().filter(this::isReviewDueOrPast).count();

        return AdminDashboardSummaryResponse.builder()
                .totalUsers(userRepository.findByRole(Role.USER).stream().filter(user -> user.getStatus() != UserStatus.DELETED).count())
                .totalDieticians(userRepository.findByRole(Role.DIETICIAN).stream().filter(user -> user.getStatus() != UserStatus.DELETED).count())
                .activeUserSubscriptions(subscriptionTransactionRepository.countByRoleTypeAndSubscriptionStatus(Role.USER, SubscriptionStatus.ACTIVE))
                .activeDieticianSubscriptions(subscriptionTransactionRepository.countByRoleTypeAndSubscriptionStatus(Role.DIETICIAN, SubscriptionStatus.ACTIVE))
                .subscriptionRevenue(revenue.getSubscriptionRevenue())
                .consultationRevenue(revenue.getConsultationRevenue())
                .adminCommissionRevenue(revenue.getAdminCommissionRevenue())
                .pendingMealCompliance(pendingMealCompliance)
                .followedMealCompliance(followedMealCompliance)
                .notFollowedMealCompliance(notFollowedMealCompliance)
                .overallMealCompliancePercent(totalMealCompliance == 0 ? 0 : Math.round((followedMealCompliance * 100f) / totalMealCompliance))
                .totalAppointments(totalAppointments)
                .pendingAppointments(appointmentRepository.countByStatus(AppointmentStatus.PENDING))
                .completedAppointments(completedAppointments)
                .cancelledAppointments(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED))
                .weeklyReviewCompletionPercent(percent(completedAppointments, dueReviews))
                .appointmentCompletionPercent(percent(completedAppointments, totalAppointments))
                .mostActiveDieticians(getMostActiveDieticians())
                .mostCompliantUsers(getMostCompliantUsers())
                .recentPayments(getPayments().stream().limit(RECENT_LIMIT).toList())
                .recentRegistrations(userRepository.findByRoleInOrderByCreatedAtDesc(List.of(Role.USER, Role.DIETICIAN))
                        .stream()
                        .filter(user -> user.getStatus() != UserStatus.DELETED)
                        .limit(RECENT_LIMIT)
                        .map(this::toUserResponse)
                        .toList())
                .build();
    }

    public List<AdminUserResponse> getUsers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.USER)
                .stream()
                .filter(user -> user.getStatus() != UserStatus.DELETED)
                .map(this::toUserResponse)
                .toList();
    }

    public List<AdminUserResponse> getDieticians() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.DIETICIAN)
                .stream()
                .filter(user -> user.getStatus() != UserStatus.DELETED)
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(String adminEmail, AdminCreateUserRequest request) {
        User admin = getAdmin(adminEmail);
        if (request.getRole() != null && request.getRole() != Role.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER");
        }

        User user = createAccount(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                Role.USER,
                validateCreateStatus(request.getStatus())
        );

        User saved = userRepository.save(user);
        recordCreateAudit(admin, saved, "ADMIN_USERS");
        return toUserResponse(saved);
    }

    @Transactional
    public AdminUserResponse createDietician(String adminEmail, AdminCreateDieticianRequest request) {
        User admin = getAdmin(adminEmail);
        User dietician = createAccount(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                Role.DIETICIAN,
                validateCreateStatus(request.getStatus())
        );
        dietician.setDegree(trim(request.getDegree()));
        dietician.setSpecialization(trim(request.getSpecialization()));
        dietician.setExperience(request.getExperience());
        dietician.setLocation(trim(request.getLocation()));
        dietician.setConsultationFee(request.getConsultationFee());
        dietician.setProfileSetupCompleted(true);

        User saved = userRepository.save(dietician);
        DieticianProfile profile = DieticianProfile.builder()
                .user(saved)
                .specialization(saved.getSpecialization())
                .experience(saved.getExperience())
                .qualification(saved.getDegree())
                .consultationFee(saved.getConsultationFee())
                .build();
        dieticianProfileRepository.save(profile);

        recordCreateAudit(admin, saved, "ADMIN_DIETICIANS");
        return toUserResponse(saved);
    }

    public List<AppointmentResponse> getAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescAppointmentTimeDesc()
                .stream()
                .map(this::toAppointmentResponse)
                .toList();
    }

    public List<AppointmentResponse> getMeetings() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescAppointmentTimeDesc()
                .stream()
                .filter(appointment -> appointment.getMeetingLink() != null && !appointment.getMeetingLink().isBlank())
                .map(this::toAppointmentResponse)
                .toList();
    }

    public List<AdminClientProgramResponse> getClientPrograms() {
        return dietPlanRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(DietPlan::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(plan -> {
                    long totalMeals = mealComplianceRepository.countByDietPlanMealDietPlan(plan);
                    long followedMeals = mealComplianceRepository.countByDietPlanMealDietPlanAndStatus(plan, MealComplianceStatus.FOLLOWED);
                    return AdminClientProgramResponse.builder()
                            .dietPlanId(plan.getId())
                            .dieticianId(plan.getDietician().getId())
                            .dieticianName(plan.getDietician().getFullName())
                            .userId(plan.getUser().getId())
                            .userName(plan.getUser().getFullName())
                            .programGoal(plan.getProgramGoal() == null || plan.getProgramGoal().isBlank() ? plan.getTitle() : plan.getProgramGoal())
                            .startDate(plan.getStartDate())
                            .currentProgress(percent(followedMeals, totalMeals))
                            .build();
                })
                .toList();
    }

    @Transactional
    public AdminUserResponse approveUser(String adminEmail, Long userId) {
        return updateUserStatus(adminEmail, userId, Role.USER, UserStatus.ACTIVE, null);
    }

    @Transactional
    public AdminUserResponse suspendUser(String adminEmail, Long userId, AdminUserActionRequest request) {
        return updateUserStatus(adminEmail, userId, Role.USER, UserStatus.SUSPENDED, request == null ? null : request.getReason());
    }

    @Transactional
    public AdminUserResponse rejectUser(String adminEmail, Long userId, AdminUserActionRequest request) {
        String reason = request == null ? null : request.getReason();
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reject reason is required");
        }
        return updateUserStatus(adminEmail, userId, Role.USER, UserStatus.REJECTED, reason);
    }

    @Transactional
    public AdminUserResponse deleteUser(String adminEmail, Long userId, AdminUserActionRequest request) {
        String reason = request == null || request.getReason() == null || request.getReason().isBlank()
                ? "Deleted by admin"
                : request.getReason();
        return updateUserStatus(adminEmail, userId, Role.USER, UserStatus.DELETED, reason);
    }

    @Transactional
    public AdminUserResponse approveDietician(String adminEmail, Long dieticianId) {
        return updateUserStatus(adminEmail, dieticianId, Role.DIETICIAN, UserStatus.ACTIVE, null);
    }

    @Transactional
    public AdminUserResponse suspendDietician(String adminEmail, Long dieticianId, AdminUserActionRequest request) {
        return updateUserStatus(adminEmail, dieticianId, Role.DIETICIAN, UserStatus.SUSPENDED, request == null ? null : request.getReason());
    }

    @Transactional
    public AdminUserResponse rejectDietician(String adminEmail, Long dieticianId, AdminUserActionRequest request) {
        String reason = request == null ? null : request.getReason();
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reject reason is required");
        }
        return updateUserStatus(adminEmail, dieticianId, Role.DIETICIAN, UserStatus.REJECTED, reason);
    }

    @Transactional
    public AdminUserResponse deleteDietician(String adminEmail, Long dieticianId, AdminUserActionRequest request) {
        String reason = request == null || request.getReason() == null || request.getReason().isBlank()
                ? "Deleted by admin"
                : request.getReason();
        return updateUserStatus(adminEmail, dieticianId, Role.DIETICIAN, UserStatus.DELETED, reason);
    }

    public List<AdminSubscriptionResponse> getSubscriptions() {
        return subscriptionTransactionRepository.findByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toSubscriptionResponse)
                .toList();
    }

    @Transactional
    public AdminSubscriptionResponse deactivateSubscription(String adminEmail, Long subscriptionId) {
        User admin = getAdmin(adminEmail);
        SubscriptionTransaction subscription = subscriptionTransactionRepository.findById(subscriptionId)
                .filter(transaction -> !transaction.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        SubscriptionTransaction saved = subscriptionTransactionRepository.save(subscription);
        syncUserSubscriptionFlag(saved.getUser());
        auditLogService.record(
                admin.getId(),
                admin.getRole(),
                "ADMIN_SUBSCRIPTION_DEACTIVATED",
                "SUBSCRIPTIONS",
                "Admin " + admin.getId() + " deactivated subscription " + saved.getId()
        );
        return toSubscriptionResponse(saved);
    }

    @Transactional
    public void deleteSubscription(String adminEmail, Long subscriptionId) {
        User admin = getAdmin(adminEmail);
        SubscriptionTransaction subscription = subscriptionTransactionRepository.findById(subscriptionId)
                .filter(transaction -> !transaction.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        subscription.setDeleted(true);
        subscription.setDeletedAt(LocalDateTime.now());
        if (subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
            subscription.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
        }
        SubscriptionTransaction saved = subscriptionTransactionRepository.save(subscription);
        syncUserSubscriptionFlag(saved.getUser());
        auditLogService.record(
                admin.getId(),
                admin.getRole(),
                "ADMIN_SUBSCRIPTION_DELETED",
                "SUBSCRIPTIONS",
                "Admin " + admin.getId() + " deleted subscription " + saved.getId()
        );
    }

    public List<AdminPaymentResponse> getPayments() {
        Stream<AdminPaymentResponse> subscriptionPayments = subscriptionTransactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toPaymentResponse);
        Stream<AdminPaymentResponse> consultationPayments = consultationPaymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toPaymentResponse);

        return Stream.concat(subscriptionPayments, consultationPayments)
                .sorted(Comparator.comparing(AdminPaymentResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<NotificationLogResponse> getNotificationLogs(Long receiverId, String channel, String status) {
        try {
            return mongoNotificationLogService.findByFilters(receiverId, channel, status)
                    .stream()
                    .filter(java.util.Objects::nonNull)
                    .map(this::toNotificationLogResponse)
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    public List<AuditLogDocument> getAuditLogs() {
        return auditLogService.findAll();
    }

    public List<ReportSnapshotDocument> getReportSnapshots() {
        return mongoReportSnapshotService.findAll();
    }

    public AdminRevenueResponse getRevenue() {
        BigDecimal subscriptionRevenue = sum(subscriptionTransactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .map(SubscriptionTransaction::getAmount));
        BigDecimal consultationRevenue = sum(consultationPaymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(ConsultationPayment::getAmount));
        BigDecimal adminCommissionRevenue = sum(consultationPaymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(ConsultationPayment::getPlatformCommissionAmount));
        BigDecimal dieticianEarningsPaid = sum(consultationPaymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(ConsultationPayment::getDieticianEarningsAmount));

        return AdminRevenueResponse.builder()
                .subscriptionRevenue(subscriptionRevenue)
                .consultationRevenue(consultationRevenue)
                .adminCommissionRevenue(adminCommissionRevenue)
                .dieticianEarningsPaid(dieticianEarningsPaid)
                .totalRevenue(subscriptionRevenue.add(adminCommissionRevenue))
                .build();
    }

    private AdminUserResponse toUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .subscriptionStatus(subscriptionService.getSubscriptionStatus(user))
                .profileImage(user.getProfileImage())
                .profileImageUrl(user.getProfileImage())
                .actionReason(user.getActionReason())
                .rejectionReason(user.getRejectionReason())
                .actionBy(user.getActionBy())
                .actionDate(user.getActionDate())
                .adminActionAt(user.getAdminActionAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User createAccount(
            String fullName,
            String email,
            String phone,
            String password,
            Role role,
            UserStatus status
    ) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        return User.builder()
                .fullName(trim(fullName))
                .email(normalizedEmail)
                .phone(trim(phone))
                .password(passwordEncoder.encode(password))
                .role(role)
                .status(status)
                .profileSetupCompleted(false)
                .subscriptionActive(false)
                .appointmentCompleted(false)
                .onboardingCompleted(false)
                .build();
    }

    private UserStatus validateCreateStatus(UserStatus status) {
        UserStatus normalizedStatus = status == null ? UserStatus.ACTIVE : status;
        if (normalizedStatus != UserStatus.ACTIVE && normalizedStatus != UserStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or INACTIVE");
        }
        return normalizedStatus;
    }

    private User getAdmin(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can perform this action");
        }
        return admin;
    }

    private void recordCreateAudit(User admin, User createdUser, String module) {
        auditLogService.record(
                admin.getId(),
                admin.getRole(),
                "ADMIN_CREATED_" + createdUser.getRole().name(),
                module,
                "Admin " + admin.getId() + " created " + createdUser.getRole() + " account " + createdUser.getId()
        );
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private AdminUserResponse updateUserStatus(String adminEmail, Long userId, Role expectedRole, UserStatus status, String reason) {
        User admin = getAdmin(adminEmail);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != expectedRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested account role does not match this action");
        }
        java.time.LocalDateTime actionAt = java.time.LocalDateTime.now();
        user.setStatus(status);
        user.setActionReason(reason);
        user.setRejectionReason(status == UserStatus.REJECTED ? reason : null);
        user.setActionBy(admin.getId());
        user.setActionDate(actionAt);
        user.setAdminActionAt(actionAt);
        User saved = userRepository.save(user);
        adminActionLogRepository.save(AdminActionLog.builder()
                .actionType(status.name())
                .targetUserId(saved.getId())
                .targetRole(expectedRole)
                .reason(reason)
                .adminId(admin.getId())
                .build());
        auditLogService.record(
                admin.getId(),
                admin.getRole(),
                "ADMIN_" + status.name(),
                expectedRole == Role.USER ? "ADMIN_USERS" : "ADMIN_DIETICIANS",
                "Admin " + admin.getId() + " set " + expectedRole + " " + saved.getId()
                        + " to " + status + (reason == null || reason.isBlank() ? "" : ": " + reason)
        );
        return toUserResponse(saved);
    }

    private AppointmentResponse toAppointmentResponse(Appointment appointment) {
        User user = appointment.getUser();
        User dietician = appointment.getDietician();
        return AppointmentResponse.builder()
                .success(appointment.getStatus() == AppointmentStatus.CANCELLED ? Boolean.TRUE : null)
                .id(appointment.getId())
                .appointmentId(appointment.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
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
                .popupMessage(appointment.getStatus() == AppointmentStatus.CANCELLED
                        && appointment.getRefundStatus() == com.nutricare.nutricarebackend.entity.RefundStatus.PENDING
                        ? cancelPopupMessage(appointment)
                        : null)
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
        String cancelledByRole = cancelledByRole(appointment);
        if (Role.DIETICIAN.name().equals(cancelledByRole)) {
            return "Appointment cancelled successfully. User and admin have been notified.";
        }
        if (Role.USER.name().equals(cancelledByRole)) {
            return "Your consultation fee will be refunded within 7 working days.";
        }
        return "Appointment cancelled successfully.";
    }

    private List<AdminActivityMetricResponse> getMostActiveDieticians() {
        return userRepository.findByRole(Role.DIETICIAN)
                .stream()
                .map(dietician -> AdminActivityMetricResponse.builder()
                        .accountId(dietician.getId())
                        .fullName(dietician.getFullName())
                        .count(appointmentRepository.countByDietician(dietician))
                        .percentage(0)
                        .build())
                .sorted(Comparator.comparing(AdminActivityMetricResponse::getCount).reversed())
                .limit(5)
                .toList();
    }

    private List<AdminActivityMetricResponse> getMostCompliantUsers() {
        return userRepository.findByRole(Role.USER)
                .stream()
                .map(user -> {
                    long total = mealComplianceRepository.countByUser(user);
                    long followed = mealComplianceRepository.countByUserAndStatus(user, MealComplianceStatus.FOLLOWED);
                    return AdminActivityMetricResponse.builder()
                            .accountId(user.getId())
                            .fullName(user.getFullName())
                            .count(followed)
                            .percentage(percent(followed, total))
                            .build();
                })
                .sorted(Comparator.comparing(AdminActivityMetricResponse::getPercentage).reversed())
                .limit(5)
                .toList();
    }

    private boolean isReviewDueOrPast(DietPlan plan) {
        if (plan.getStartDate() == null) {
            return false;
        }
        long dayNumber = ChronoUnit.DAYS.between(plan.getStartDate(), LocalDate.now()) + 1;
        return dayNumber >= 6;
    }

    private int percent(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return Math.round((numerator * 100f) / denominator);
    }

    private AdminSubscriptionResponse toSubscriptionResponse(SubscriptionTransaction subscription) {
        User account = subscription.getUser();
        SubscriptionPlan plan = subscription.getPlan();

        return AdminSubscriptionResponse.builder()
                .id(subscription.getId())
                .accountId(account.getId())
                .fullName(account.getFullName())
                .userName(account.getFullName())
                .email(account.getEmail())
                .role(subscription.getRoleType())
                .planId(plan.getId())
                .planName(plan.getName())
                .price(plan.getPrice())
                .amount(subscription.getAmount())
                .status(subscription.getSubscriptionStatus())
                .subscriptionStatus(subscription.getSubscriptionStatus())
                .paymentStatus(subscription.getStatus())
                .deleted(subscription.isDeleted())
                .deletedAt(subscription.getDeletedAt())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    private void syncUserSubscriptionFlag(User user) {
        boolean hasActive = subscriptionTransactionRepository
                .findByUserAndSubscriptionStatus(user, SubscriptionStatus.ACTIVE)
                .stream()
                .anyMatch(transaction -> !transaction.isDeleted());
        user.setSubscriptionActive(hasActive);
        userRepository.save(user);
    }

    private AdminPaymentResponse toPaymentResponse(SubscriptionTransaction payment) {
        User payer = payment.getUser();
        SubscriptionPlan plan = payment.getPlan();

        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .paymentType("SUBSCRIPTION")
                .payerId(payer.getId())
                .payerName(payer.getFullName())
                .payerEmail(payer.getEmail())
                .payerRole(payer.getRole())
                .planName(plan.getName())
                .amount(payment.getAmount())
                .adminCommission(BigDecimal.ZERO)
                .dieticianEarnings(BigDecimal.ZERO)
                .paymentStatus(payment.getStatus())
                .paymentStatusText(paymentStatusText(payment.getStatus()))
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private AdminPaymentResponse toPaymentResponse(ConsultationPayment payment) {
        User payer = payment.getUser();

        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .paymentType("CONSULTATION")
                .payerId(payer.getId())
                .payerName(payer.getFullName())
                .payerEmail(payer.getEmail())
                .payerRole(payer.getRole())
                .planName(null)
                .amount(payment.getAmount())
                .adminCommission(payment.getPlatformCommissionAmount())
                .dieticianEarnings(payment.getDieticianEarningsAmount())
                .paymentStatus(payment.getPaymentStatus())
                .paymentStatusText(paymentStatusText(payment.getPaymentStatus()))
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private NotificationLogResponse toNotificationLogResponse(NotificationLogDocument log) {
        User receiver = log.getReceiverId() == null ? null : userRepository.findById(log.getReceiverId()).orElse(null);
        return NotificationLogResponse.builder()
                .id(log.getMysqlLogId())
                .userId(log.getReceiverId())
                .receiverName(receiver != null ? receiver.getFullName() : log.getReceiverName())
                .receiverEmail(receiver != null ? receiver.getEmail() : log.getReceiverEmail())
                .receiverRole(log.getReceiverRole())
                .receiverPhone(log.getReceiverPhone())
                .channel(log.getChannel())
                .title(log.getTitle())
                .message(log.getMessage())
                .status(log.getStatus())
                .twilioSid(log.getTwilioSid())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .sentAt(log.getUpdatedAt())
                .build();
    }

    private String paymentStatusText(PaymentStatus status) {
        if (status == PaymentStatus.SUCCESS) {
            return "Paid / Success";
        }
        if (status == PaymentStatus.PENDING) {
            return "Not Paid / Pending";
        }
        return status == null ? "Not Paid / Pending" : status.name();
    }

    private BigDecimal sum(Stream<BigDecimal> values) {
        return values
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public SystemStatsResponse getSystemStats() {
        java.io.File root = new java.io.File(".");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;

        double totalGb = (double) totalSpace / (1024.0 * 1024.0 * 1024.0);
        double usedGb = (double) usedSpace / (1024.0 * 1024.0 * 1024.0);
        double usedPercent = totalSpace > 0 ? ((double) usedSpace / totalSpace) * 100.0 : 0.0;

        return SystemStatsResponse.builder()
                .storageUsedGb(Math.round(usedGb * 10.0) / 10.0)
                .storageTotalGb(Math.round(totalGb * 10.0) / 10.0)
                .storageUsedPercent(Math.round(usedPercent * 10.0) / 10.0)
                .build();
    }

    @Transactional
    public DiseaseResponse createDisease(String adminEmail, DiseaseRequest request) {
        User admin = getAdmin(adminEmail);
        DiseaseResponse response = diseaseService.createDisease(request);
        auditLogService.record(
                admin.getId(),
                admin.getRole(),
                "ADMIN_CREATED_DISEASE",
                "DISEASES",
                "Admin " + admin.getId() + " created disease: " + request.getName()
        );
        return response;
    }

    @Transactional
    public DiseaseResponse updateDisease(String adminEmail, Long id, DiseaseRequest request) {
        User admin = getAdmin(adminEmail);
        DiseaseResponse response = diseaseService.updateDisease(id, request);
        auditLogService.record(
                admin.getId(),
                admin.getRole(),
                "ADMIN_UPDATED_DISEASE",
                "DISEASES",
                "Admin " + admin.getId() + " updated disease ID: " + id
        );
        return response;
    }

    @Transactional
    public void deleteDisease(String adminEmail, Long id) {
        User admin = getAdmin(adminEmail);
        diseaseService.deleteDisease(id);
        auditLogService.record(
                admin.getId(),
                admin.getRole(),
                "ADMIN_DELETED_DISEASE",
                "DISEASES",
                "Admin " + admin.getId() + " deleted disease ID: " + id
        );
    }
}
