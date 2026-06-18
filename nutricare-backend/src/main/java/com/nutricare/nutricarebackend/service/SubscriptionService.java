package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.PaymentTransactionResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentVerifyRequest;
import com.nutricare.nutricarebackend.dto.FeaturePermissionsResponse;
import com.nutricare.nutricarebackend.dto.SubscriptionConfirmRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionConfirmResponse;
import com.nutricare.nutricarebackend.dto.SubscriptionCreateRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionCreateResponse;
import com.nutricare.nutricarebackend.dto.SubscriptionPlanRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionPlanResponse;
import com.nutricare.nutricarebackend.dto.UserSubscriptionResponse;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionPlan;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.SubscriptionTransaction;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.SubscriptionPlanRepository;
import com.nutricare.nutricarebackend.repository.SubscriptionTransactionRepository;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private static final String CURRENCY = "INR";
    private static final String PAYMENT_PROVIDER = "RAZORPAY";
    public static final String UPGRADE_MESSAGE = "Please subscribe or upgrade your plan to access this feature.";

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;
    private final BillService billService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @PostConstruct
    public void logRazorpayConfig() {
        System.out.println("Razorpay Key Loaded: " + razorpayKeyId);
    }

    public List<SubscriptionPlanResponse> getActivePlans(String email) {
        log.info("SubscriptionService.getActivePlans entered. Email: {}", email);
        try {
            log.info("Database query started: Fetching active subscription plans");
            List<SubscriptionPlan> plans = subscriptionPlanRepository.findByActiveTrueOrderByPriceAsc();
            int recordCount = plans == null ? 0 : plans.size();
            log.info("Database query completed. Record count: {}", recordCount);
            if (plans == null || plans.isEmpty()) {
                return List.of();
            }
            log.info("DTO mapping started for active subscription plans");
            return plans.stream()
                    .map(plan -> {
                        try {
                            return toPlanResponse(plan);
                        } catch (Exception ex) {
                            log.error("Error mapping subscription plan record id={}: {}", plan.getId(), ex.getMessage(), ex);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getActivePlans. Full exception: ", e);
            return List.of();
        }
    }

    public List<SubscriptionPlanResponse> getAllPlansForAdmin() {
        log.info("SubscriptionService.getAllPlansForAdmin entered");
        try {
            log.info("Database query started: Fetching all subscription plans for admin");
            List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
            int recordCount = plans == null ? 0 : plans.size();
            log.info("Database query completed. Record count: {}", recordCount);
            if (plans == null || plans.isEmpty()) {
                return List.of();
            }
            log.info("DTO mapping started for admin subscription plans");
            return plans.stream()
                    .map(plan -> {
                        try {
                            return toPlanResponse(plan);
                        } catch (Exception ex) {
                            log.error("Error mapping subscription plan record id={}: {}", plan.getId(), ex.getMessage(), ex);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getAllPlansForAdmin. Full exception: ", e);
            return List.of();
        }
    }

    public SubscriptionPlanResponse createPlanForAdmin(SubscriptionPlanRequest request) {
        Role planAudience = request.getEffectiveAudience();
        if (planAudience != Role.USER && planAudience != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plans can only be created for USER or DIETICIAN");
        }
        if (subscriptionPlanRepository.existsByNameAndRoleType(request.getName(), planAudience.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription plan already exists for this audience");
        }

        SubscriptionPlan saved = subscriptionPlanRepository.save(SubscriptionPlan.builder()
                .name(request.getName())
                .roleType(planAudience.name())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .features(request.getFeatures())
                .active(request.isActive())
                .canBookAppointment(bool(request.getCanBookAppointment()))
                .canVideoCall(bool(request.getCanVideoCall()))
                .videoCallLimitMinutes(request.getVideoCallLimitMinutes())
                .canMealLogs(bool(request.getCanMealLogs()))
                .canFollowUps(bool(request.getCanFollowUps()))
                .canChat(bool(request.getCanChat()))
                .allowedUserPlans(request.getAllowedUserPlans())
                .maxUsers(request.getMaxUsers())
                .maxAppointments(request.getMaxAppointments())
                .build());
        auditLogService.record(
                null,
                Role.ADMIN,
                "SUBSCRIPTION_PLAN_UPDATED",
                "SUBSCRIPTIONS",
                "Subscription plan " + saved.getId() + " created or updated"
        );

        return toPlanResponse(saved);
    }

    public SubscriptionPlanResponse updatePlanForAdmin(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription plan not found"));
        Role planAudience = request.getEffectiveAudience();
        if (planAudience != Role.USER && planAudience != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plans can only be created for USER or DIETICIAN");
        }

        plan.setName(request.getName());
        plan.setRoleType(planAudience.name());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setFeatures(request.getFeatures());
        plan.setActive(request.isActive());
        plan.setCanBookAppointment(bool(request.getCanBookAppointment()));
        plan.setCanVideoCall(bool(request.getCanVideoCall()));
        plan.setVideoCallLimitMinutes(request.getVideoCallLimitMinutes());
        plan.setCanMealLogs(bool(request.getCanMealLogs()));
        plan.setCanFollowUps(bool(request.getCanFollowUps()));
        plan.setCanChat(bool(request.getCanChat()));
        plan.setAllowedUserPlans(request.getAllowedUserPlans());
        plan.setMaxUsers(request.getMaxUsers());
        plan.setMaxAppointments(request.getMaxAppointments());

        SubscriptionPlan saved = subscriptionPlanRepository.save(plan);
        auditLogService.record(
                null,
                Role.ADMIN,
                "SUBSCRIPTION_PLAN_UPDATED",
                "SUBSCRIPTIONS",
                "Subscription plan " + saved.getId() + " updated"
        );
        return toPlanResponse(saved);
    }

    public SubscriptionPlanResponse deactivatePlanForAdmin(Long id) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription plan not found"));
        plan.setActive(false);
        SubscriptionPlan saved = subscriptionPlanRepository.save(plan);
        auditLogService.record(null, Role.ADMIN, "SUBSCRIPTION_PLAN_UPDATED", "SUBSCRIPTIONS", "Subscription plan " + saved.getId() + " deactivated");
        return toPlanResponse(saved);
    }

    public SubscriptionPlanResponse activatePlanForAdmin(Long id) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription plan not found"));
        plan.setActive(true);
        SubscriptionPlan saved = subscriptionPlanRepository.save(plan);
        auditLogService.record(null, Role.ADMIN, "SUBSCRIPTION_PLAN_UPDATED", "SUBSCRIPTIONS", "Subscription plan " + saved.getId() + " activated");
        return toPlanResponse(saved);
    }

    public void deletePlanForAdmin(Long id) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription plan not found"));
        plan.setActive(false);
        SubscriptionPlan saved = subscriptionPlanRepository.save(plan);
        auditLogService.record(null, Role.ADMIN, "SUBSCRIPTION_PLAN_UPDATED", "SUBSCRIPTIONS", "Subscription plan " + saved.getId() + " deleted/deactivated");
    }

    public SubscriptionCreateResponse createSubscriptionTransaction(String email, SubscriptionCreateRequest request) {
        validateSubscriptionCreateRequest(request);
        User authenticatedUser = getAuthenticatedUser(email);
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!authenticatedUser.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create payment order for another user");
        }
        requireSubscriberRole(user);

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        if (plan.getRoleType() == null || !plan.getRoleType().equalsIgnoreCase(user.getRole().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription plan is not available for this role");
        }

        SubscriptionTransaction transaction = SubscriptionTransaction.builder()
                .user(user)
                .plan(plan)
                .roleType(user.getRole())
                .amount(plan.getPrice())
                .status(PaymentStatus.PENDING)
                .subscriptionStatus(SubscriptionStatus.INACTIVE)
                .paymentProvider(PAYMENT_PROVIDER)
                .build();

        long amountInPaise = toPaise(plan.getPrice());
        log.info(
                "Subscription create-order validated: planId={}, userId={}, amountInPaise={}, razorpayMode={}",
                plan.getId(),
                user.getId(),
                amountInPaise,
                razorpayEnvironment()
        );

        SubscriptionTransaction saved = saveSubscriptionTransaction(transaction, "CREATE_PENDING_SUBSCRIPTION");
        saved.setRazorpayOrderId(createRazorpayOrder(saved));
        saved = saveSubscriptionTransaction(saved, "STORE_RAZORPAY_ORDER_ID");

        return SubscriptionCreateResponse.builder()
                .transaction(toPaymentResponse(saved))
                .plan(toPlanResponse(plan))
                .orderId(saved.getRazorpayOrderId())
                .razorpayOrderId(saved.getRazorpayOrderId())
                .userId(user.getId())
                .key(razorpayKeyId)
                .currency(CURRENCY)
                .build();
    }

    @Transactional
    public SubscriptionConfirmResponse confirmSubscription(String email, SubscriptionConfirmRequest request) {
        User user = getAuthenticatedUser(email);
        requireSubscriberRole(user);
        validateSubscriptionConfirmRequest(request);

        SubscriptionTransaction transaction = subscriptionTransactionRepository.findByIdAndUser(request.getTransactionId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment record not found"));

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment already verified");
        }
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not pending verification");
        }

        SubscriptionPlan plan = transaction.getPlan();
        if (plan.getRoleType() == null || !plan.getRoleType().equalsIgnoreCase(user.getRole().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription plan is not available for this role");
        }

        if (!request.getRazorpayOrderId().equals(transaction.getRazorpayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order does not match subscription transaction");
        }

        boolean signatureMatched = isValidRazorpaySignature(request);
        log.info(
                "Subscription payment verification result: localPaymentId={}, razorpayOrderId={}, razorpayPaymentId={}, verified={}",
                transaction.getId(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                signatureMatched
        );
        log.info(
                "Subscription payment signature match: userId={}, localPaymentId={}, planId={}, orderId={}, paymentId={}, matched={}",
                user.getId(),
                transaction.getId(),
                plan.getId(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                signatureMatched
        );

        if (!signatureMatched) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setProviderPaymentId(request.getRazorpayPaymentId());
            transaction.setRazorpayOrderId(request.getRazorpayOrderId());
            transaction.setRazorpaySignature(request.getRazorpaySignature());
            saveSubscriptionTransaction(transaction, "INVALID_SIGNATURE");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature");
        }

        subscriptionTransactionRepository.findByUserAndSubscriptionStatus(user, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(active -> !active.isDeleted())
                .forEach(active -> {
                    active.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                    saveSubscriptionTransaction(active, "EXPIRE_ACTIVE_SUBSCRIPTION");
                });

        LocalDate startDate = LocalDate.now();
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        transaction.setProviderPaymentId(request.getRazorpayPaymentId());
        transaction.setRazorpayOrderId(request.getRazorpayOrderId());
        transaction.setRazorpaySignature(request.getRazorpaySignature());
        transaction.setStartDate(startDate);
        transaction.setEndDate(startDate.plusDays(plan.getDurationDays()));
        SubscriptionTransaction savedTransaction = saveSubscriptionTransaction(transaction, "ACTIVATE_SUBSCRIPTION");
        billService.createBillForSubscription(savedTransaction);
        user.setSubscriptionActive(true);
        user.setOnboardingCompleted(isOnboardingComplete(user));
        saveUserAfterSubscriptionVerification(user, savedTransaction);
        sendSubscriptionNotification(user, plan, savedTransaction);
        auditLogService.record(
                user.getId(),
                user.getRole(),
                "SUBSCRIPTION_PAYMENT_SUCCESS",
                "SUBSCRIPTIONS",
                "Subscription payment " + savedTransaction.getId() + " succeeded for user " + user.getId()
        );

        return SubscriptionConfirmResponse.builder()
                .success(true)
                .message("Subscription activated successfully")
                .transaction(toPaymentResponse(savedTransaction))
                .subscription(toSubscriptionResponse(savedTransaction))
                .build();
    }

    @Transactional
    public SubscriptionConfirmResponse verifySubscriptionPayment(String email, ConsultationPaymentVerifyRequest request) {
        User user = getAuthenticatedUser(email);
        requireSubscriberRole(user);
        validateSubscriptionVerifyRequest(request);
        if (request.getUserId() != null && !request.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot verify another user's payment");
        }

        boolean signatureMatched = isValidRazorpaySignature(request);
        log.info(
                "Subscription payment signature match: userId={}, localPaymentId={}, planId={}, orderId={}, paymentId={}, matched={}",
                user.getId(),
                request.getPaymentId(),
                request.getPlanId(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                signatureMatched
        );
        if (!signatureMatched) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature");
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription plan not found"));
        if (plan.getRoleType() == null || !plan.getRoleType().equalsIgnoreCase(user.getRole().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription plan is not available for this role");
        }

        SubscriptionTransaction transaction = subscriptionTransactionRepository
                .findById(request.getPaymentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription payment record not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot verify another user's payment");
        }
        if (!transaction.getPlan().getId().equals(plan.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription plan does not match payment");
        }
        if (!request.getRazorpayOrderId().equals(transaction.getRazorpayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order does not match subscription transaction");
        }
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment already verified");
        }
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not pending verification");
        }

        log.info(
                "Subscription payment verification started: userId={}, localPaymentId={}, planId={}, orderId={}, paymentId={}",
                user.getId(),
                transaction.getId(),
                plan.getId(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId()
        );

        subscriptionTransactionRepository.findByUserAndSubscriptionStatus(user, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(active -> !active.isDeleted())
                .forEach(active -> {
                    active.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                    saveSubscriptionTransaction(active, "EXPIRE_ACTIVE_SUBSCRIPTION");
                });

        LocalDate startDate = LocalDate.now();
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        transaction.setProviderPaymentId(request.getRazorpayPaymentId());
        transaction.setRazorpayOrderId(request.getRazorpayOrderId());
        transaction.setRazorpaySignature(request.getRazorpaySignature());
        transaction.setStartDate(startDate);
        transaction.setEndDate(startDate.plusDays(plan.getDurationDays()));
        SubscriptionTransaction savedTransaction = saveSubscriptionTransaction(transaction, "ACTIVATE_SUBSCRIPTION");
        billService.createBillForSubscription(savedTransaction);

        user.setSubscriptionActive(true);
        user.setOnboardingCompleted(isOnboardingComplete(user));
        saveUserAfterSubscriptionVerification(user, savedTransaction);
        sendSubscriptionNotification(user, plan, savedTransaction);
        auditLogService.record(
                user.getId(),
                user.getRole(),
                "SUBSCRIPTION_PAYMENT_SUCCESS",
                "SUBSCRIPTIONS",
                "Subscription payment " + savedTransaction.getId() + " succeeded for user " + user.getId()
        );

        return SubscriptionConfirmResponse.builder()
                .success(true)
                .message("Subscription activated successfully")
                .transaction(toPaymentResponse(savedTransaction))
                .subscription(toSubscriptionResponse(savedTransaction))
                .build();
    }

    public UserSubscriptionResponse getMySubscription(String email) {
        User user = getAuthenticatedUser(email);
        requireSubscriberRole(user);
        return getCurrentSubscription(user);
    }

    public FeaturePermissionsResponse getMyFeatures(String email) {
        log.info("SubscriptionService.getMyFeatures entered. Email: {}", email);
        try {
            if (email == null) {
                log.info("Email is null, returning default features");
                return defaultFeatures(null, null);
            }
            log.info("Database query started: Fetching authenticated user");
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.warn("Authenticated user not found for email: {}, returning default features", email);
                return defaultFeatures(null, null);
            }
            log.info("User found: id={}, role={}", user.getId(), user.getRole());
            if (user.getRole() == Role.ADMIN) {
                log.info("DTO mapping started for ADMIN permissions");
                return FeaturePermissionsResponse.builder()
                        .userId(user.getId())
                        .role(user.getRole())
                        .subscriptionStatus(SubscriptionStatus.ACTIVE)
                        .canBookAppointment(true)
                        .canVideoCall(true)
                        .videoCallLimitMinutes(120)
                        .canMealLogs(true)
                        .canFollowUps(true)
                        .canChat(true)
                        .canHealthTracking(true)
                        .build();
            }
            log.info("Query started: Fetching active subscription for user: {}", user.getId());
            UserSubscriptionResponse subscription = getCurrentSubscription(user);
            log.info("Subscription queried. Active status: {}", subscription != null ? subscription.getStatus() : "None");
            if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                log.info("No active subscription exists, returning default features");
                return defaultFeatures(user.getId(), user.getRole());
            }
            log.info("DTO mapping started: Mapping active subscription features to response");
            return FeaturePermissionsResponse.builder()
                    .userId(user.getId())
                    .role(user.getRole())
                    .planId(subscription.getPlanId())
                    .planName(subscription.getPlanName())
                    .subscriptionStatus(subscription.getStatus())
                    .canBookAppointment(subscription.isCanBookAppointment())
                    .canVideoCall(subscription.isCanVideoCall())
                    .videoCallLimitMinutes(subscription.getVideoCallLimitMinutes() != null ? subscription.getVideoCallLimitMinutes() : 0)
                    .canMealLogs(subscription.isCanMealLogs())
                    .canFollowUps(subscription.isCanFollowUps())
                    .canChat(subscription.isCanChat())
                    .allowedUserPlans(subscription.getAllowedUserPlans() != null ? subscription.getAllowedUserPlans() : "")
                    .maxUsers(subscription.getMaxUsers() != null ? subscription.getMaxUsers() : 0)
                    .canHealthTracking(true)
                    .build();
        } catch (Exception e) {
            log.error("Error in getMyFeatures. Full exception: ", e);
            try {
                if (email != null) {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        return defaultFeatures(user.getId(), user.getRole());
                    }
                }
            } catch (Exception ignored) {}
            return defaultFeatures(null, null);
        }
    }

    private FeaturePermissionsResponse defaultFeatures(Long userId, Role role) {
        return FeaturePermissionsResponse.builder()
                .userId(userId)
                .role(role)
                .subscriptionStatus(SubscriptionStatus.INACTIVE)
                .canBookAppointment(false)
                .canVideoCall(false)
                .videoCallLimitMinutes(0)
                .canMealLogs(false)
                .canFollowUps(false)
                .canChat(false)
                .canHealthTracking(true)
                .build();
    }

    public UserSubscriptionResponse getCurrentSubscription(User user) {
        if (user == null) {
            return inactiveSubscription(null);
        }
        try {
            return subscriptionTransactionRepository.findByUserAndSubscriptionStatus(user, SubscriptionStatus.ACTIVE)
                    .stream()
                    .filter(this::isCurrent)
                    .filter(subscription -> subscription != null && !subscription.isDeleted())
                    .max(java.util.Comparator.comparing(subscription -> subscription.getCreatedAt() != null ? subscription.getCreatedAt() : java.time.LocalDateTime.MIN))
                    .map(this::toSubscriptionResponse)
                    .orElseGet(() -> inactiveSubscription(user));
        } catch (Exception e) {
            log.error("Error in getCurrentSubscription for user id={}: {}", user.getId(), e.getMessage(), e);
            return inactiveSubscription(user);
        }
    }

    public SubscriptionStatus getSubscriptionStatus(User user) {
        if (user.getRole() != Role.USER && user.getRole() != Role.DIETICIAN) {
            return SubscriptionStatus.ACTIVE;
        }
        return getCurrentSubscription(user).getStatus();
    }

    public void requireActiveSubscription(String email) {
        User user = getAuthenticatedUser(email);
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        requireSubscriberRole(user);
        if (getSubscriptionStatus(user) != SubscriptionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, UPGRADE_MESSAGE);
        }
    }

    public void requireFeature(String email, String feature) {
        User user = getAuthenticatedUser(email);
        requireFeature(user, feature);
    }

    public void requireFeature(User user, String feature) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        requireSubscriberRole(user);
        UserSubscriptionResponse subscription = getCurrentSubscription(user);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE || !hasFeature(subscription, feature)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, UPGRADE_MESSAGE);
        }
    }

    public boolean hasFeature(User user, String feature) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() != Role.USER && user.getRole() != Role.DIETICIAN) {
            return false;
        }
        UserSubscriptionResponse subscription = getCurrentSubscription(user);
        return subscription.getStatus() == SubscriptionStatus.ACTIVE && hasFeature(subscription, feature);
    }

    private boolean hasFeature(UserSubscriptionResponse subscription, String feature) {
        return switch (feature) {
            case "BOOK_APPOINTMENT" -> subscription.isCanBookAppointment();
            case "VIDEO_CALL" -> subscription.isCanVideoCall();
            case "MEAL_LOGS" -> subscription.isCanMealLogs();
            case "FOLLOW_UPS" -> subscription.isCanFollowUps();
            case "CHAT" -> subscription.isCanChat();
            default -> false;
        };
    }

    private boolean isCurrent(SubscriptionTransaction subscription) {
        return subscription.getEndDate() != null && !subscription.getEndDate().isBefore(LocalDate.now());
    }

    private void validateSubscriptionCreateRequest(SubscriptionCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment order request is required");
        }
        if (request.getPlanId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planId is required");
        }
        if (request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        validateRazorpayCredentials();
    }

    private void validateSubscriptionConfirmRequest(SubscriptionConfirmRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification request is required");
        }
        if (request.getTransactionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required for subscription payment verification");
        }
        validatePaymentFields(
                request.getRazorpayPaymentId(),
                request.getRazorpayOrderId(),
                request.getRazorpaySignature()
        );
        validateRazorpayCredentials();
    }

    private void validateSubscriptionVerifyRequest(ConsultationPaymentVerifyRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification request is required");
        }
        if (request.getPlanId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planId is required for subscription payment verification");
        }
        if (request.getPaymentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId is required for subscription payment verification");
        }
        validatePaymentFields(
                request.getRazorpayPaymentId(),
                request.getRazorpayOrderId(),
                request.getRazorpaySignature()
        );
        validateRazorpayCredentials();
    }

    private void validatePaymentFields(String paymentId, String orderId, String signature) {
        if (!hasText(paymentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "razorpayPaymentId is required");
        }
        if (!hasText(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "razorpayOrderId is required");
        }
        if (!hasText(signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "razorpaySignature is required");
        }
    }

    private void sendSubscriptionNotification(User user, SubscriptionPlan plan, SubscriptionTransaction transaction) {
        try {
            notificationService.sendNotification(
                    user,
                    user,
                    "Subscription Activated",
                    "Your NutriCare subscription has been activated successfully.",
                    "SUBSCRIPTION_ACTIVATED"
            );
        } catch (RuntimeException ex) {
            log.warn(
                    "Subscription notification skipped after successful verification: userId={}, transactionId={}, error={}",
                    user.getId(),
                    transaction.getId(),
                    ex.getClass().getSimpleName()
            );
        }
    }

    private SubscriptionTransaction saveSubscriptionTransaction(SubscriptionTransaction transaction, String phase) {
        try {
            return subscriptionTransactionRepository.save(transaction);
        } catch (RuntimeException ex) {
            log.error(
                    "Subscription payment DB save failed: phase={}, transactionId={}, userId={}, planId={}, orderId={}, error={}",
                    phase,
                    transaction == null ? null : transaction.getId(),
                    transaction == null || transaction.getUser() == null ? null : transaction.getUser().getId(),
                    transaction == null || transaction.getPlan() == null ? null : transaction.getPlan().getId(),
                    transaction == null ? null : transaction.getRazorpayOrderId(),
                    ex.getMessage(),
                    ex
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save subscription payment", ex);
        }
    }

    private void saveUserAfterSubscriptionVerification(User user, SubscriptionTransaction transaction) {
        try {
            userRepository.save(user);
        } catch (RuntimeException ex) {
            log.error(
                    "Subscription user update failed after payment verification: userId={}, transactionId={}, orderId={}, error={}",
                    user == null ? null : user.getId(),
                    transaction == null ? null : transaction.getId(),
                    transaction == null ? null : transaction.getRazorpayOrderId(),
                    ex.getMessage(),
                    ex
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to activate user subscription", ex);
        }
    }

    private boolean isOnboardingComplete(User user) {
        if (user.getRole() == Role.USER || user.getRole() == Role.DIETICIAN) {
            return user.isProfileSetupCompleted() && user.isSubscriptionActive() && appointmentCompleted(user);
        }
        return true;
    }

    private boolean appointmentCompleted(User user) {
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

    private User getAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireSubscriberRole(User user) {
        if (user.getRole() != Role.USER && user.getRole() != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only users and dieticians can manage subscriptions");
        }
    }

    private String createRazorpayOrder(SubscriptionTransaction transaction) {
        validateRazorpayCredentials();
        long amountInPaise = toPaise(transaction.getAmount());
        String receipt = "sub_" + transaction.getUser().getId() + "_" + System.currentTimeMillis();
        log.info(
                "Razorpay order creation started: paymentType=SUBSCRIPTION, environment={}, transactionId={}, planId={}, userId={}, amountRupees={}, amountPaise={}, currency={}, receipt={}",
                razorpayEnvironment(),
                transaction.getId(),
                transaction.getPlan().getId(),
                transaction.getUser().getId(),
                transaction.getAmount(),
                amountInPaise,
                CURRENCY,
                receipt
        );
        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", CURRENCY);
            orderRequest.put("receipt", receipt);

            Order order = razorpayClient.orders.create(orderRequest);
            return order.get("id");
        } catch (RazorpayException | ArithmeticException ex) {
            log.error(
                    "Razorpay order creation failed: paymentType=SUBSCRIPTION, transactionId={}, planId={}, userId={}, amountPaise={}, error={}",
                    transaction.getId(),
                    transaction.getPlan().getId(),
                    transaction.getUser().getId(),
                    amountInPaise,
                    ex.getMessage()
            );
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create Razorpay order", ex);
        }
    }

    private void validateRazorpayCredentials() {
        if (isMissingRazorpayValue(razorpayKeyId) || isMissingRazorpayValue(razorpayKeySecret)) {
            log.error("Razorpay credentials not configured");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Razorpay credentials not configured");
        }
        if (razorpayEnvironment().equals("UNKNOWN")) {
            log.error("Razorpay key id has invalid format: keyPrefix={}", safeKeyPrefix());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Razorpay key id is invalid");
        }
        log.debug("Razorpay environment resolved: {}", razorpayEnvironment());
    }

    private boolean isMissingRazorpayValue(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim();
        return normalized.equals("rzp_" + "test_key")
                || normalized.equals("rzp_" + "test_secret")
                || normalized.startsWith("YOUR_");
    }

    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String razorpayEnvironment() {
        if (razorpayKeyId == null) {
            return "UNKNOWN";
        }
        if (razorpayKeyId.startsWith("rzp_" + "test_")) {
            return "TEST";
        }
        if (razorpayKeyId.startsWith("rzp_" + "live_")) {
            return "LIVE";
        }
        return "UNKNOWN";
    }

    private String safeKeyPrefix() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank()) {
            return null;
        }
        int visibleLength = Math.min(9, razorpayKeyId.length());
        return razorpayKeyId.substring(0, visibleLength);
    }

    private boolean isValidRazorpaySignature(SubscriptionConfirmRequest request) {
        return isValidRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
    }

    private boolean isValidRazorpaySignature(ConsultationPaymentVerifyRequest request) {
        return isValidRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
    }

    private boolean isValidRazorpaySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expectedSignature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    razorpaySignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify Razorpay signature", ex);
        }
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        if (plan == null) return null;
        log.info("Mapping SubscriptionPlan to DTO. Plan ID: {}", plan.getId());
        String rType = plan.getRoleType();
        Role audience = null;
        if (rType != null) {
            try {
                audience = Role.valueOf(rType.toUpperCase());
            } catch (Exception e) {
                audience = Role.USER;
            }
        }

        Integer maxAppts = plan.getMaxAppointments();
        if (maxAppts == null) {
            maxAppts = (audience == Role.DIETICIAN) ? -1 : 0;
        }

        Integer maxUsrs = plan.getMaxUsers();
        if (maxUsrs == null) {
            maxUsrs = (audience == Role.DIETICIAN) ? -1 : 0;
        }

        Integer videoCallLimit = plan.getVideoCallLimitMinutes();
        if (videoCallLimit == null) {
            videoCallLimit = 0;
        }

        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getName())
                .name(plan.getName())
                .planAudience(audience)
                .roleType(plan.getRoleType())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationInDays(plan.getDurationDays())
                .durationDays(plan.getDurationDays())
                .features(plan.getFeatures())
                .active(plan.isActive())
                .canBookAppointment(Boolean.TRUE.equals(plan.getCanBookAppointment()))
                .canVideoCall(Boolean.TRUE.equals(plan.getCanVideoCall()))
                .videoCallLimitMinutes(videoCallLimit)
                .canMealLogs(Boolean.TRUE.equals(plan.getCanMealLogs()))
                .canFollowUps(Boolean.TRUE.equals(plan.getCanFollowUps()))
                .canChat(Boolean.TRUE.equals(plan.getCanChat()))
                .allowedUserPlans(plan.getAllowedUserPlans() != null ? plan.getAllowedUserPlans() : "")
                .maxUsers(maxUsrs)
                .maxAppointments(maxAppts)
                .build();
    }

    private PaymentTransactionResponse toPaymentResponse(SubscriptionTransaction transaction) {
        SubscriptionPlan plan = transaction.getPlan();
        String rType = plan.getRoleType();
        Role audience = null;
        if (rType != null) {
            try {
                audience = Role.valueOf(rType.toUpperCase());
            } catch (Exception e) {
                audience = Role.USER;
            }
        }

        return PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .planId(plan.getId())
                .planName(plan.getName())
                .planAudience(audience)
                .amount(transaction.getAmount())
                .paymentStatus(transaction.getStatus())
                .paymentStatusText(paymentStatusText(transaction.getStatus()))
                .subscriptionStatus(transaction.getSubscriptionStatus())
                .paymentProvider(transaction.getPaymentProvider())
                .providerPaymentId(transaction.getProviderPaymentId())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .razorpaySignature(transaction.getRazorpaySignature())
                .startDate(transaction.getStartDate())
                .endDate(transaction.getEndDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private UserSubscriptionResponse toSubscriptionResponse(SubscriptionTransaction subscription) {
        SubscriptionPlan plan = subscription.getPlan();
        if (plan == null) {
            return inactiveSubscription(subscription.getUser());
        }
        log.info("Mapping SubscriptionTransaction to DTO. Plan ID: {}", plan.getId());
        String rType = plan.getRoleType();
        Role audience = null;
        if (rType != null) {
            try {
                audience = Role.valueOf(rType.toUpperCase());
            } catch (Exception e) {
                audience = Role.USER;
            }
        }

        Integer maxAppts = plan.getMaxAppointments();
        if (maxAppts == null) {
            maxAppts = (audience == Role.DIETICIAN) ? -1 : 0;
        }

        Integer maxUsrs = plan.getMaxUsers();
        if (maxUsrs == null) {
            maxUsrs = (audience == Role.DIETICIAN) ? -1 : 0;
        }

        Integer videoCallLimit = plan.getVideoCallLimitMinutes();
        if (videoCallLimit == null) {
            videoCallLimit = 0;
        }

        return UserSubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .planId(plan.getId())
                .planName(plan.getName())
                .planAudience(audience)
                .planDescription(plan.getDescription())
                .price(plan.getPrice())
                .durationInDays(plan.getDurationDays())
                .features(plan.getFeatures())
                .status(subscription.getSubscriptionStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .createdAt(subscription.getCreatedAt())
                .canBookAppointment(Boolean.TRUE.equals(plan.getCanBookAppointment()))
                .canVideoCall(Boolean.TRUE.equals(plan.getCanVideoCall()))
                .videoCallLimitMinutes(videoCallLimit)
                .canMealLogs(Boolean.TRUE.equals(plan.getCanMealLogs()))
                .canFollowUps(Boolean.TRUE.equals(plan.getCanFollowUps()))
                .canChat(Boolean.TRUE.equals(plan.getCanChat()))
                .allowedUserPlans(plan.getAllowedUserPlans() != null ? plan.getAllowedUserPlans() : "")
                .maxUsers(maxUsrs)
                .maxAppointments(maxAppts)
                .build();
    }

    private UserSubscriptionResponse inactiveSubscription(User user) {
        return UserSubscriptionResponse.builder()
                .userId(user.getId())
                .planAudience(user.getRole())
                .status(SubscriptionStatus.INACTIVE)
                .build();
    }

    private boolean bool(Boolean value) {
        return Boolean.TRUE.equals(value);
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
}
