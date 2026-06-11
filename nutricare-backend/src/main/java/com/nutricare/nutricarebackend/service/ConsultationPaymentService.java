package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.ConsultationPaymentConfirmRequest;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentCreateOrderRequest;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentCreateOrderResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentVerifyRequest;
import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.BookingStatus;
import com.nutricare.nutricarebackend.entity.ConsultationPayment;
import com.nutricare.nutricarebackend.entity.DieticianProfile;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.ConsultationPaymentRepository;
import com.nutricare.nutricarebackend.repository.DieticianProfileRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationPaymentService {

    private static final String CURRENCY = "INR";
    private static final String PAYMENT_PROVIDER = "RAZORPAY";
    private static final BigDecimal PLATFORM_COMMISSION_RATE = new BigDecimal("10.00");
    private static final BigDecimal COMMISSION_DIVISOR = new BigDecimal("100.00");

    private final ConsultationPaymentRepository consultationPaymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final DieticianProfileRepository dieticianProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final BillService billService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @PostConstruct
    public void validateRazorpayConfig() {
        System.out.println("=== Razorpay Config ===");
        System.out.println("Razorpay Key Loaded: " + razorpayKeyId);
        System.out.println("Secret Loaded: " + (razorpayKeySecret != null && !razorpayKeySecret.isBlank()));
    }

    @Transactional
    public ConsultationPaymentCreateOrderResponse createOrder(
            String email,
            ConsultationPaymentCreateOrderRequest request
    ) {
        User user = getAuthenticatedUser(email);
        requireRole(user, Role.USER, "Only users can create consultation payments");
        if (request.getAppointmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointmentId is required for consultation payments");
        }
        log.info("Create payment order request: email={}, userId={}, appointmentId={}",
                email,
                user.getId(),
                request.getAppointmentId());

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getUser().getId().equals(user.getId())) {
            log.warn("Create payment order denied: userId={} does not own appointmentId={} ownerUserId={}",
                    user.getId(),
                    appointment.getId(),
                    appointment.getUser().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot pay for another user's appointment");
        }

        ConsultationPayment payment = consultationPaymentRepository.findByAppointment(appointment)
                .map(this::resetFailedPayment)
                .orElseGet(() -> createPendingPayment(user, appointment));
        long amountInPaise = toPaise(payment.getAmount());
        String razorpayOrderId = createRazorpayOrder(payment);
        payment.setRazorpayOrderId(razorpayOrderId);
        appointment.setOrderId(razorpayOrderId);
        appointmentRepository.save(appointment);
        ConsultationPayment savedPayment = consultationPaymentRepository.save(payment);

        return ConsultationPaymentCreateOrderResponse.builder()
                .paymentType("CONSULTATION")
                .appointmentId(appointment.getId())
                .amount(BigDecimal.valueOf(amountInPaise))
                .currency(CURRENCY)
                .paymentId(savedPayment.getId())
                .orderId(savedPayment.getRazorpayOrderId())
                .razorpayOrderId(savedPayment.getRazorpayOrderId())
                .key(razorpayKeyId)
                .paymentStatus(savedPayment.getPaymentStatus())
                .build();
    }

    @Transactional
    public ConsultationPaymentResponse confirmPayment(String email, ConsultationPaymentConfirmRequest request) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use /api/payments/verify to confirm Razorpay payments");
    }

    @Transactional
    public ConsultationPaymentResponse verifyPayment(String email, ConsultationPaymentVerifyRequest request) {
        User user = getAuthenticatedUser(email);
        requireRole(user, Role.USER, "Only users can verify consultation payments");
        if (request == null) {
            logPaymentVerificationReject("REQUEST_MISSING", null, null, user, null, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification request is required");
        }
        if (request.getPaymentId() == null) {
            logPaymentVerificationReject("PAYMENT_ID_MISSING", request, null, user, null, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId is required for appointment payment verification");
        }
        if (request.getAppointmentId() == null) {
            logPaymentVerificationReject("APPOINTMENT_ID_MISSING", request, null, user, null, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointmentId is required for appointment payment verification");
        }

        validateConsultationVerifyRequest(request, null, user);

        ConsultationPayment payment = consultationPaymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> {
                    logPaymentVerificationReject("PAYMENT_RECORD_NOT_FOUND", request, null, user, null, null, false);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation payment not found");
                });
        Appointment appointment = payment.getAppointment();
        if (appointment == null) {
            logPaymentVerificationReject("APPOINTMENT_NOT_FOUND", request, null, user, payment, null, false);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        log.info(
                "Appointment payment verification request received: paymentId={}, appointmentId={}, requestOrderId={}, dbAppointmentOrderId={}, dbPaymentOrderId={}, razorpayPaymentIdPresent={}, signaturePresent={}, loggedInUserId={}, appointmentUserId={}",
                payment.getId(),
                appointment.getId(),
                request.getRazorpayOrderId(),
                appointment.getOrderId(),
                payment.getRazorpayOrderId(),
                hasText(request.getRazorpayPaymentId()),
                hasText(request.getRazorpaySignature()),
                user.getId(),
                appointment.getUser().getId()
        );
        if (!request.getAppointmentId().equals(appointment.getId())) {
            logPaymentVerificationReject("PAYMENT_APPOINTMENT_MISMATCH", request, appointment, user, payment, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment id mismatch");
        }
        if (!appointment.getUser().getId().equals(user.getId())) {
            logPaymentVerificationReject("APPOINTMENT_USER_MISMATCH", request, appointment, user, null, null, false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot verify another user's appointment payment");
        }

        if (!payment.getUser().getId().equals(user.getId())) {
            logPaymentVerificationReject("PAYMENT_USER_MISMATCH", request, appointment, user, payment, null, false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot verify another user's payment");
        }

        if (!request.getRazorpayOrderId().equals(payment.getRazorpayOrderId())) {
            logPaymentVerificationReject("PAYMENT_ORDER_MISMATCH", request, appointment, user, payment, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order id mismatch");
        }

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info(
                    "Consultation payment already verified: appointmentId={}, localPaymentId={}, razorpayOrderId={}, razorpayPaymentId={}",
                    payment.getAppointment().getId(),
                    payment.getId(),
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId()
            );
            return toResponse(payment, "Payment already verified");
        }
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            logPaymentVerificationReject("PAYMENT_NOT_PENDING", request, appointment, user, payment, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not pending verification");
        }

        boolean signatureMatched = isValidRazorpaySignature(request);
        log.info(
                "Appointment payment signature validation: appointmentId={}, requestOrderId={}, dbAppointmentOrderId={}, paymentIdPresent={}, signaturePresent={}, loggedInUserId={}, appointmentUserId={}, signatureValid={}",
                appointment.getId(),
                request.getRazorpayOrderId(),
                appointment.getOrderId(),
                hasText(request.getRazorpayPaymentId()),
                hasText(request.getRazorpaySignature()),
                user.getId(),
                appointment.getUser().getId(),
                signatureMatched
        );
        if (!signatureMatched) {
            logInvalidRazorpaySignature(request);
            logPaymentVerificationReject("INVALID_SIGNATURE", request, appointment, user, payment, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setProviderPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpayOrderId(request.getRazorpayOrderId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaidAt(paidAt);

        appointment.setBookingStatus(BookingStatus.CONFIRMED);
        appointment.setPaymentStatus(PaymentStatus.SUCCESS);
        appointment.setConsultationFee(payment.getAmount());
        appointment.setPaymentId(request.getRazorpayPaymentId());
        appointment.setOrderId(request.getRazorpayOrderId());
        appointment.setPaidAt(paidAt);
        appointment.setStatus(AppointmentStatus.APPROVED);
        saveAppointmentAfterPaymentVerification(appointment, payment);
        user.setAppointmentCompleted(true);
        user.setOnboardingCompleted(user.isProfileSetupCompleted() && user.isSubscriptionActive());
        userRepository.save(user);
        User dietician = appointment.getDietician();
        dietician.setAppointmentCompleted(true);
        dietician.setOnboardingCompleted(dietician.isProfileSetupCompleted() && dietician.isSubscriptionActive());
        userRepository.save(dietician);

        ConsultationPayment savedPayment = saveConsultationPayment(payment, "CONFIRM_PAYMENT");
        billService.createBillForConsultation(savedPayment);
        sendConsultationPaymentNotifications(user, appointment, savedPayment);
        auditLogService.record(
                user.getId(),
                user.getRole(),
                "CONSULTATION_PAYMENT_SUCCESS",
                "PAYMENT",
                "Consultation payment " + savedPayment.getId() + " succeeded for appointment " + appointment.getId()
        );

        return toResponse(savedPayment, "Appointment confirmed. Dietician has been notified.");
    }

    public List<ConsultationPaymentResponse> getMyPayments(String email) {
        User user = getAuthenticatedUser(email);
        requireRole(user, Role.USER, "Only users can view their consultation payments");

        return consultationPaymentRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ConsultationPaymentResponse> getDieticianPayments(String email) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can view their consultation payments");

        return consultationPaymentRepository.findByDieticianOrderByCreatedAtDesc(dietician)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ConsultationPayment createPendingPayment(User user, Appointment appointment) {
        DieticianProfile profile = dieticianProfileRepository.findByUser(appointment.getDietician())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dietician profile not found"));
        BigDecimal rawConsultationFee = profile.getConsultationFee() != null
                ? profile.getConsultationFee()
                : appointment.getDietician().getConsultationFee();
        if (rawConsultationFee == null || rawConsultationFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dietician consultation fee is not configured");
        }
        BigDecimal consultationFee = rawConsultationFee.setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformCommission = consultationFee
                .multiply(PLATFORM_COMMISSION_RATE)
                .divide(COMMISSION_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal dieticianEarnings = consultationFee.subtract(platformCommission).setScale(2, RoundingMode.HALF_UP);

        if (appointment.getBookingStatus() != BookingStatus.RESCHEDULED) {
            appointment.setBookingStatus(BookingStatus.APPROVED);
        }
        appointment.setPaymentStatus(PaymentStatus.PENDING);
        appointment.setConsultationFee(consultationFee);
        appointmentRepository.save(appointment);

        ConsultationPayment payment = ConsultationPayment.builder()
                .user(user)
                .dietician(appointment.getDietician())
                .appointment(appointment)
                .amount(consultationFee)
                .platformCommissionRate(PLATFORM_COMMISSION_RATE)
                .platformCommissionAmount(platformCommission)
                .dieticianEarningsAmount(dieticianEarnings)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentProvider(PAYMENT_PROVIDER)
                .build();

        return consultationPaymentRepository.save(payment);
    }

    private ConsultationPayment resetFailedPayment(ConsultationPayment payment) {
        if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
            payment.setPaymentStatus(PaymentStatus.PENDING);
            payment.setProviderPaymentId(null);
            payment.setRazorpayOrderId(null);
            payment.setRazorpaySignature(null);
            payment.setPaidAt(null);
            return consultationPaymentRepository.save(payment);
        }

        return payment;
    }

    private String createRazorpayOrder(ConsultationPayment payment) {
        validateRazorpayCredentials();
        long amountInPaise = toPaise(payment.getAmount());
        String receipt = "consultation_" + payment.getId();
        log.info(
                "Razorpay order creation started: paymentType=CONSULTATION, environment={}, appointmentId={}, amountRupees={}, amountPaise={}, currency={}, receipt={}",
                razorpayEnvironment(),
                payment.getAppointment().getId(),
                payment.getAmount(),
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
                    "Razorpay order creation failed: paymentType=CONSULTATION, appointmentId={}, amountPaise={}, error={}",
                    payment.getAppointment().getId(),
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

    private void validateConsultationVerifyRequest(ConsultationPaymentVerifyRequest request, Appointment appointment, User user) {
        if (!hasText(request.getRazorpayPaymentId())) {
            logPaymentVerificationReject("RAZORPAY_PAYMENT_ID_MISSING", request, appointment, user, null, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay verification fields");
        }
        if (!hasText(request.getRazorpayOrderId())) {
            logPaymentVerificationReject("RAZORPAY_ORDER_ID_MISSING", request, appointment, user, null, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay verification fields");
        }
        if (!hasText(request.getRazorpaySignature())) {
            logPaymentVerificationReject("RAZORPAY_SIGNATURE_MISSING", request, appointment, user, null, null, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay verification fields");
        }
        validateRazorpayCredentials();
    }

    private void logPaymentVerificationReject(
            String reason,
            ConsultationPaymentVerifyRequest request,
            Appointment appointment,
            User user,
            ConsultationPayment payment,
            Boolean signatureValid,
            boolean success
    ) {
        log.warn(
                "Appointment payment verification rejected: reason={}, appointmentId={}, requestOrderId={}, dbAppointmentOrderId={}, dbPaymentOrderId={}, paymentIdPresent={}, signaturePresent={}, loggedInUserId={}, appointmentUserId={}, signatureValid={}, success={}",
                reason,
                request == null ? null : request.getAppointmentId(),
                request == null ? null : request.getRazorpayOrderId(),
                appointment == null ? null : appointment.getOrderId(),
                payment == null ? null : payment.getRazorpayOrderId(),
                request != null && hasText(request.getRazorpayPaymentId()),
                request != null && hasText(request.getRazorpaySignature()),
                user == null ? null : user.getId(),
                appointment == null || appointment.getUser() == null ? null : appointment.getUser().getId(),
                signatureValid,
                success
        );
    }

    private void validatePaymentFields(String paymentId, String orderId, String signature) {
        if (!hasText(paymentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay verification fields");
        }
        if (!hasText(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay verification fields");
        }
        if (!hasText(signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay verification fields");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ConsultationPayment saveConsultationPayment(ConsultationPayment payment, String phase) {
        try {
            return consultationPaymentRepository.save(payment);
        } catch (RuntimeException ex) {
            log.error(
                    "Consultation payment DB save failed: phase={}, paymentId={}, userId={}, appointmentId={}, orderId={}, error={}",
                    phase,
                    payment == null ? null : payment.getId(),
                    payment == null || payment.getUser() == null ? null : payment.getUser().getId(),
                    payment == null || payment.getAppointment() == null ? null : payment.getAppointment().getId(),
                    payment == null ? null : payment.getRazorpayOrderId(),
                    ex.getMessage(),
                    ex
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save consultation payment", ex);
        }
    }

    private void saveAppointmentAfterPaymentVerification(Appointment appointment, ConsultationPayment payment) {
        try {
            appointmentRepository.save(appointment);
        } catch (RuntimeException ex) {
            log.error(
                    "Appointment update failed after payment verification: appointmentId={}, paymentId={}, orderId={}, error={}",
                    appointment == null ? null : appointment.getId(),
                    payment == null ? null : payment.getId(),
                    payment == null ? null : payment.getRazorpayOrderId(),
                    ex.getMessage(),
                    ex
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to confirm appointment payment", ex);
        }
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

    private void sendConsultationPaymentNotifications(User user, Appointment appointment, ConsultationPayment savedPayment) {
        try {
            notificationService.sendNotification(
                    user,
                    appointment.getDietician(),
                    "Payment Success",
                    "Payment successful.\n\n"
                            + "Amount:\n₹" + savedPayment.getAmount() + "\n\n"
                            + "Appointment:\n" + appointment.getId(),
                    "PAYMENT_SUCCESS"
            );
            notificationService.sendNotification(
                    appointment.getDietician(),
                    user,
                    "Appointment Confirmed",
                    "New paid appointment booked.",
                    "APPOINTMENT_CONFIRMED"
            );
        } catch (RuntimeException ex) {
            log.warn(
                    "Consultation payment notification skipped after successful verification: userId={}, paymentId={}, appointmentId={}, error={}",
                    user.getId(),
                    savedPayment.getId(),
                    appointment.getId(),
                    ex.getClass().getSimpleName()
            );
        }

        try {
            User dietician = appointment.getDietician();
            String formattedDate = appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            String formattedTime = appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("hh:mm a"));

            String userMsg = "NutriCare: Your appointment with Dietician " + dietician.getFullName() + " is confirmed on " + formattedDate + " at " + formattedTime + ".";
            String dieticianMsg = "NutriCare: Appointment with " + user.getFullName() + " is confirmed on " + formattedDate + " at " + formattedTime + ".";

            // User SMS
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                try {
                    notificationService.sendSms(user, "Appointment Confirmed", userMsg);
                } catch (Exception ex) {
                    log.error("Failed to send SMS confirmation to user {}: {}", user.getId(), ex.getMessage());
                }
            } else {
                log.info("Skipping SMS confirmation for user {} because phone is missing.", user.getId());
            }

            // Dietician SMS
            if (dietician.getPhone() != null && !dietician.getPhone().isBlank()) {
                try {
                    notificationService.sendSms(dietician, "Appointment Confirmed", dieticianMsg);
                } catch (Exception ex) {
                    log.error("Failed to send SMS confirmation to dietician {}: {}", dietician.getId(), ex.getMessage());
                }
            } else {
                log.info("Skipping SMS confirmation for dietician {} because phone is missing.", dietician.getId());
            }
        } catch (Exception ex) {
            log.error("Error in Twilio confirmation flow: {}", ex.getMessage());
        }
    }

    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private boolean isValidRazorpaySignature(ConsultationPaymentVerifyRequest request) {
        try {
            String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
            return Utils.verifySignature(
                    payload,
                    request.getRazorpaySignature(),
                    razorpayKeySecret
            );
        } catch (RazorpayException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify Razorpay signature", ex);
        }
    }

    private void logInvalidRazorpaySignature(ConsultationPaymentVerifyRequest request) {
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        System.out.println("Invalid Razorpay signature");
        System.out.println("payload used = " + payload);
        System.out.println("order id = " + request.getRazorpayOrderId());
        System.out.println("payment id = " + request.getRazorpayPaymentId());
        System.out.println("signature = " + request.getRazorpaySignature());
        System.out.println("secret loaded = " + (razorpayKeySecret != null && !razorpayKeySecret.isBlank()));
        log.warn(
                "Invalid Razorpay signature: payload={}, orderId={}, paymentId={}, signature={}, secretLoaded={}",
                payload,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                razorpayKeySecret != null && !razorpayKeySecret.isBlank()
        );
    }

    private User getAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private ConsultationPaymentResponse toResponse(ConsultationPayment payment) {
        return toResponse(payment, null);
    }

    private ConsultationPaymentResponse toResponse(ConsultationPayment payment, String message) {
        User user = payment.getUser();
        User dietician = payment.getDietician();

        return ConsultationPaymentResponse.builder()
                .success(message == null ? null : true)
                .message(message)
                .id(payment.getId())
                .appointmentId(payment.getAppointment().getId())
                .appointmentStatus(payment.getAppointment().getStatus())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .dieticianId(dietician.getId())
                .dieticianFullName(dietician.getFullName())
                .amount(payment.getAmount())
                .platformCommissionRate(payment.getPlatformCommissionRate())
                .platformCommissionAmount(payment.getPlatformCommissionAmount())
                .dieticianEarningsAmount(payment.getDieticianEarningsAmount())
                .currency(CURRENCY)
                .paymentStatus(payment.getPaymentStatus())
                .paymentStatusText(paymentStatusText(payment.getPaymentStatus()))
                .paymentProvider(payment.getPaymentProvider())
                .providerPaymentId(payment.getProviderPaymentId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpaySignature(payment.getRazorpaySignature())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private String paymentStatusText(PaymentStatus status) {
        if (status == null) return "Not Paid";
        return status == PaymentStatus.SUCCESS ? "Paid" : "Not Paid";
    }
}
