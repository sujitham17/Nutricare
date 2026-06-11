package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.AppointmentPaymentVerifyRequest;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentConfirmRequest;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentCreateOrderRequest;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentCreateOrderResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentVerifyRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionCreateRequest;
import com.nutricare.nutricarebackend.dto.SubscriptionCreateResponse;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.SubscriptionPlan;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.nutricare.nutricarebackend.repository.SubscriptionPlanRepository;
import com.nutricare.nutricarebackend.service.ConsultationPaymentService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ConsultationPaymentController {

    private final ConsultationPaymentService consultationPaymentService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @PostMapping("/api/payments/create-order")
    public ResponseEntity<?> createOrder(
            Authentication authentication,
            @Valid @RequestBody ConsultationPaymentCreateOrderRequest request
    ) {
        Long userId = null;
        String role = null;
        Long planId = request != null ? request.getPlanId() : null;
        Boolean planExists = null;
        BigDecimal planPrice = null;
        Long amountInPaise = null;
        boolean razorpayKeyExists = (razorpayKeyId != null && !razorpayKeyId.isBlank() && !razorpayKeyId.equals("{RAZORPAY_KEY_ID}"));

        try {
            logAuthentication("POST /api/payments/create-order", authentication);

            // 1. Authorization check
            if (authentication == null || authentication.getName() == null || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
                log.info("Payments create-order: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}",
                        userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists);
                return failure(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }

            User loggedInUser = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (loggedInUser == null) {
                log.info("Payments create-order: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}",
                        userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists);
                return failure(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }

            userId = loggedInUser.getId();
            role = loggedInUser.getRole() != null ? loggedInUser.getRole().name() : null;

            // 2. Razorpay configuration check (applies to both subscription and appointment)
            boolean keyMissing = (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeyId.equals("{RAZORPAY_KEY_ID}") || razorpayKeyId.startsWith("YOUR_"));
            boolean secretMissing = (razorpayKeySecret == null || razorpayKeySecret.isBlank() || razorpayKeySecret.equals("{RAZORPAY_KEY_SECRET}") || razorpayKeySecret.startsWith("YOUR_"));
            if (keyMissing || secretMissing) {
                log.error("Razorpay configuration missing: keyMissing={}, secretMissing={}", keyMissing, secretMissing);
                return failure(HttpStatus.INTERNAL_SERVER_ERROR, "Razorpay configuration missing");
            }

            boolean isSubscriptionFlow = request.getPlanId() != null || (request.getPaymentType() != null && "SUBSCRIPTION".equalsIgnoreCase(request.getPaymentType()));

            if (isSubscriptionFlow) {
                // 3. planId check
                if (planId == null) {
                    log.info("Payments create-order: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}",
                            userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists);
                    return failure(HttpStatus.BAD_REQUEST, "planId is required");
                }

                // 4. Plan existence check
                SubscriptionPlan plan = subscriptionPlanRepository.findById(planId).orElse(null);
                planExists = plan != null;
                if (!planExists) {
                    log.info("Payments create-order: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}",
                            userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists);
                    return failure(HttpStatus.NOT_FOUND, "Plan not found");
                }

                planPrice = plan.getPrice();

                // 5. Plan price validation
                if (planPrice == null || planPrice.compareTo(BigDecimal.ZERO) < 0) {
                    log.info("Payments create-order: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}",
                            userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists);
                    return failure(HttpStatus.BAD_REQUEST, "Invalid plan price");
                }

                amountInPaise = planPrice.movePointRight(2).longValue();

                // Log detailed parameters for valid state
                log.info("Payments create-order: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}",
                        userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists);

                SubscriptionCreateRequest subscriptionRequest = new SubscriptionCreateRequest();
                subscriptionRequest.setPlanId(planId);
                subscriptionRequest.setUserId(userId);
                subscriptionRequest.setPaymentProvider("RAZORPAY");

                SubscriptionCreateResponse subscriptionOrder = subscriptionService.createSubscriptionTransaction(
                        authentication.getName(),
                        subscriptionRequest
                );

                Map<String, Object> responseBody = new LinkedHashMap<>();
                responseBody.put("success", true);
                responseBody.put("orderId", subscriptionOrder.getRazorpayOrderId());
                responseBody.put("razorpayOrderId", subscriptionOrder.getRazorpayOrderId());
                responseBody.put("transactionId", subscriptionOrder.getTransaction().getId());
                responseBody.put("amount", amountInPaise);
                responseBody.put("currency", "INR");
                responseBody.put("key", razorpayKeyId);
                responseBody.put("planId", planId);

                return ResponseEntity.ok(responseBody);
            }

            // Otherwise, it is an appointment payment flow.
            return ResponseEntity.ok(consultationPaymentService.createOrder(authentication.getName(), request));

        } catch (ResponseStatusException ex) {
            log.error("Payment create-order exception: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}, error={}",
                    userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists, ex.getReason() != null ? ex.getReason() : ex.getMessage(), ex);
            return failure(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
        } catch (IllegalArgumentException ex) {
            log.error("Payment create-order exception: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}, error={}",
                    userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists, ex.getMessage(), ex);
            return failure(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.error("Payment create-order exception: userId={}, role={}, planId={}, plan exists={}, plan price={}, amountInPaise={}, Razorpay key exists={}, error={}",
                    userId, role, planId, planExists, planPrice, amountInPaise, razorpayKeyExists, ex.getMessage(), ex);
            return failure(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/api/payments/confirm")
    public ResponseEntity<ConsultationPaymentResponse> confirmPayment(
            Authentication authentication,
            @Valid @RequestBody ConsultationPaymentConfirmRequest request
    ) {
        logAuthentication("POST /api/payments/confirm", authentication);
        return ResponseEntity.ok(consultationPaymentService.confirmPayment(authentication.getName(), request));
    }

    @PostMapping("/api/payments/verify")
    public ResponseEntity<?> verifyPayment(
            Authentication authentication,
            @Valid @RequestBody ConsultationPaymentVerifyRequest request
    ) {
        try {
            logAuthentication("POST /api/payments/verify", authentication);
            if (authentication == null || authentication.getName() == null) {
                return failure(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            if (request == null) {
                return failure(HttpStatus.BAD_REQUEST, "Missing Razorpay verification fields");
            }
            log.info(
                    "Payment verify request body: paymentId={}, appointmentId={}, transactionId={}, paymentType={}, razorpayOrderId={}, razorpayPaymentId={}, signaturePresent={}, planId={}, userId={}",
                    localPaymentId(request),
                    appointmentId(request),
                    transactionId(request),
                    paymentType(request),
                    razorpayOrderId(request),
                    razorpayPaymentId(request),
                    razorpaySignaturePresent(request),
                    planId(request),
                    userId(request)
            );

            if (isSubscriptionPayment(request)) {
                return ResponseEntity.ok(subscriptionService.verifySubscriptionPayment(
                        authentication.getName(),
                        request
                ));
            }

            if (isAppointmentPayment(request)) {
                return ResponseEntity.ok(consultationPaymentService.verifyPayment(authentication.getName(), request));
            }

            return failure(HttpStatus.BAD_REQUEST, "Missing or unsupported payment type");
        } catch (ResponseStatusException ex) {
            return failure(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
        } catch (IllegalArgumentException ex) {
            log.warn("Payment verification request rejected: {}", ex.getMessage());
            return failure(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                    "Unexpected payment verification failure: paymentId={}, appointmentId={}, transactionId={}, paymentType={}, razorpayOrderId={}, razorpayPaymentId={}, planId={}, userId={}, error={}, message={}",
                    localPaymentId(request),
                    appointmentId(request),
                    transactionId(request),
                    paymentType(request),
                    razorpayOrderId(request),
                    razorpayPaymentId(request),
                    planId(request),
                    userId(request),
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
            return failure(HttpStatus.BAD_REQUEST, "Unable to verify payment");
        }
    }

    @PostMapping("/api/payments/verify-appointment")
    public ResponseEntity<?> verifyAppointmentPayment(
            Authentication authentication,
            @Valid @RequestBody AppointmentPaymentVerifyRequest request
    ) {
        System.out.println("===== VERIFY APPOINTMENT PAYMENT START =====");
        System.out.println("Request body: " + request);
        System.out.println("paymentId = " + (request == null ? null : request.getPaymentId()));
        System.out.println("appointmentId = " + (request == null ? null : request.getAppointmentId()));
        System.out.println("razorpayOrderId = " + (request == null ? null : request.getRazorpayOrderId()));
        System.out.println("razorpayPaymentId = " + (request == null ? null : request.getRazorpayPaymentId()));
        System.out.println("signature exists = " + (request != null && request.getRazorpaySignature() != null));
        ConsultationPaymentVerifyRequest verifyRequest = toConsultationPaymentVerifyRequest(request);
        try {
            logAuthentication("POST /api/payments/verify-appointment", authentication);
            if (authentication == null || authentication.getName() == null) {
                return failure(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            if (request == null) {
                return failure(HttpStatus.BAD_REQUEST, "Payment verification request is required");
            }
            if (request.getPaymentId() == null) {
                return failure(HttpStatus.BAD_REQUEST, "Missing paymentId");
            }
            if (request.getAppointmentId() == null) {
                return failure(HttpStatus.BAD_REQUEST, "Missing appointmentId");
            }
            if (isBlank(request.getRazorpayOrderId())) {
                return failure(HttpStatus.BAD_REQUEST, "Missing razorpayOrderId");
            }
            if (isBlank(request.getRazorpayPaymentId())) {
                return failure(HttpStatus.BAD_REQUEST, "Missing razorpayPaymentId");
            }
            if (isBlank(request.getRazorpaySignature())) {
                return failure(HttpStatus.BAD_REQUEST, "Missing razorpaySignature");
            }
            log.info(
                    "Appointment payment verify request: appointmentId={}, razorpayOrderId={}, razorpayPaymentId={}, signaturePresent={}",
                    request.getAppointmentId(),
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    razorpaySignaturePresent(request)
            );
            return ResponseEntity.ok(consultationPaymentService.verifyPayment(authentication.getName(), verifyRequest));
        } catch (ResponseStatusException ex) {
            return failure(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
        } catch (IllegalArgumentException ex) {
            log.warn("Appointment payment verification request rejected: {}", ex.getMessage());
            return failure(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                    "Unexpected appointment payment verification failure: appointmentId={}, razorpayOrderId={}, razorpayPaymentId={}, error={}, message={}",
                    appointmentId(verifyRequest),
                    razorpayOrderId(verifyRequest),
                    razorpayPaymentId(verifyRequest),
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
            return failure(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/api/payments/verify-subscription")
    public ResponseEntity<?> verifySubscriptionPayment(
            Authentication authentication,
            @Valid @RequestBody ConsultationPaymentVerifyRequest request
    ) {
        try {
            logAuthentication("POST /api/payments/verify-subscription", authentication);
            if (authentication == null || authentication.getName() == null) {
                return failure(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            if (request == null) {
                return failure(HttpStatus.BAD_REQUEST, "Payment verification request is required");
            }
            request.setPaymentType("SUBSCRIPTION");
            log.info(
                    "Subscription payment verify request: paymentId={}, planId={}, userId={}, razorpayOrderId={}, razorpayPaymentId={}, signaturePresent={}",
                    localPaymentId(request),
                    planId(request),
                    userId(request),
                    razorpayOrderId(request),
                    razorpayPaymentId(request),
                    razorpaySignaturePresent(request)
            );
            return ResponseEntity.ok(subscriptionService.verifySubscriptionPayment(authentication.getName(), request));
        } catch (ResponseStatusException ex) {
            return failure(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
        } catch (IllegalArgumentException ex) {
            log.warn("Subscription payment verification request rejected: {}", ex.getMessage());
            return failure(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                    "Unexpected subscription payment verification failure: paymentId={}, planId={}, userId={}, razorpayOrderId={}, razorpayPaymentId={}, error={}, message={}",
                    localPaymentId(request),
                    planId(request),
                    userId(request),
                    razorpayOrderId(request),
                    razorpayPaymentId(request),
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
            return failure(HttpStatus.BAD_REQUEST, "Unable to verify subscription payment");
        }
    }

    @GetMapping("/api/payments/my")
    public ResponseEntity<List<ConsultationPaymentResponse>> getMyPayments(Authentication authentication) {
        logAuthentication("GET /api/payments/my", authentication);
        return ResponseEntity.ok(consultationPaymentService.getMyPayments(authentication.getName()));
    }

    @GetMapping("/api/payments/dietician")
    public ResponseEntity<List<ConsultationPaymentResponse>> getDieticianPayments(Authentication authentication) {
        logAuthentication("GET /api/payments/dietician", authentication);
        return ResponseEntity.ok(consultationPaymentService.getDieticianPayments(authentication.getName()));
    }

    private void logAuthentication(String endpoint, Authentication authentication) {
        log.info(
                "{} authentication name={}, authorities={}",
                endpoint,
                authentication == null ? null : authentication.getName(),
                authentication == null ? null : authentication.getAuthorities()
        );
    }

    private ConsultationPaymentCreateOrderResponse toPaymentOrderResponse(SubscriptionCreateResponse response) {
        return ConsultationPaymentCreateOrderResponse.builder()
                .paymentType("SUBSCRIPTION")
                .transactionId(response.getTransaction().getId())
                .planId(response.getPlan().getId())
                .userId(response.getUserId())
                .amount(BigDecimal.valueOf(toPaise(response.getTransaction().getAmount())))
                .currency(response.getCurrency())
                .orderId(response.getOrderId())
                .razorpayOrderId(response.getRazorpayOrderId())
                .key(response.getKey())
                .paymentStatus(response.getTransaction().getPaymentStatus())
                .build();
    }

    private ConsultationPaymentVerifyRequest toConsultationPaymentVerifyRequest(AppointmentPaymentVerifyRequest request) {
        if (request == null) {
            return null;
        }
        ConsultationPaymentVerifyRequest verifyRequest = new ConsultationPaymentVerifyRequest();
        verifyRequest.setPaymentType("APPOINTMENT");
        verifyRequest.setPaymentId(request.getPaymentId());
        verifyRequest.setAppointmentId(request.getAppointmentId());
        verifyRequest.setRazorpayOrderId(request.getRazorpayOrderId());
        verifyRequest.setRazorpayPaymentId(request.getRazorpayPaymentId());
        verifyRequest.setRazorpaySignature(request.getRazorpaySignature());
        return verifyRequest;
    }

    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private String razorpayPaymentId(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getRazorpayPaymentId();
    }

    private String razorpayOrderId(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getRazorpayOrderId();
    }

    private boolean razorpaySignaturePresent(ConsultationPaymentVerifyRequest request) {
        return request != null && request.getRazorpaySignature() != null && !request.getRazorpaySignature().isBlank();
    }

    private boolean razorpaySignaturePresent(AppointmentPaymentVerifyRequest request) {
        return request != null && request.getRazorpaySignature() != null && !request.getRazorpaySignature().isBlank();
    }

    private Long localPaymentId(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getPaymentId();
    }

    private Long appointmentId(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getAppointmentId();
    }

    private Long transactionId(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getTransactionId();
    }

    private String paymentType(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getPaymentType();
    }

    private Long planId(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getPlanId();
    }

    private Long userId(ConsultationPaymentVerifyRequest request) {
        return request == null ? null : request.getUserId();
    }

    private boolean isSubscriptionPayment(ConsultationPaymentVerifyRequest request) {
        return request != null && "SUBSCRIPTION".equalsIgnoreCase(request.getPaymentType());
    }

    private boolean isAppointmentPayment(ConsultationPaymentVerifyRequest request) {
        if (request == null) {
            return false;
        }
        return request.getAppointmentId() != null
                || "APPOINTMENT".equalsIgnoreCase(request.getPaymentType())
                || "CONSULTATION".equalsIgnoreCase(request.getPaymentType());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseEntity<Map<String, Object>> failure(HttpStatus status, String message) {
        String responseMessage = message == null || message.isBlank()
                ? status.getReasonPhrase()
                : message;
        return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "message", responseMessage
        ));
    }
}
