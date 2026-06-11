package com.nutricare.nutricarebackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricare.nutricarebackend.dto.AuthResponse;
import com.nutricare.nutricarebackend.dto.ForgotPasswordOtpRequest;
import com.nutricare.nutricarebackend.dto.GoogleLoginRequest;
import com.nutricare.nutricarebackend.dto.LoginRequest;
import com.nutricare.nutricarebackend.dto.RegisterRequest;
import com.nutricare.nutricarebackend.dto.ResetPasswordOtpRequest;
import com.nutricare.nutricarebackend.dto.UserResponse;
import com.nutricare.nutricarebackend.dto.VerifyPasswordOtpRequest;
import com.nutricare.nutricarebackend.dto.RegisterSendOtpRequest;
import com.nutricare.nutricarebackend.dto.RegisterVerifyRequest;
import com.nutricare.nutricarebackend.dto.ResetPasswordTokenRequest;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.PasswordResetOtp;
import com.nutricare.nutricarebackend.entity.PasswordResetToken;
import com.nutricare.nutricarebackend.entity.OtpPurpose;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.PasswordResetOtpRepository;
import com.nutricare.nutricarebackend.repository.PasswordResetTokenRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.nutricare.nutricarebackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SubscriptionService subscriptionService;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final SmsService smsService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${google.client.id:${nutricare.google.client-id:}}")
    private String googleClientId;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${nutricare.auth.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${nutricare.auth.otp-resend-seconds:60}")
    private long otpResendSeconds;

    @Transactional
    public void sendRegisterOtp(RegisterSendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Register OTP request received");
        if (!otpService.isValidGmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid Gmail address.");
        }
        log.info("Email validated");
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        log.info("User already exists check done");
        String otp = otpService.generateAndSaveOtp(email, OtpPurpose.REGISTER);
        log.info("OTP generated");
        log.info("OTP saved");

        String subject = "NutriCare Registration Verification OTP";
        String body = "Your NutriCare registration OTP is " + otp + ". It expires in 5 minutes.";
        log.info("Email sending started");
        try {
            emailService.sendEmail(email, subject, body);
            log.info("Email sent successfully");
        } catch (Exception ex) {
            log.error("Register OTP email failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    public AuthResponse verifyRegisterOtp(RegisterVerifyRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!otpService.isValidGmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid Gmail address.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        boolean verified = otpService.verifyOtp(email, request.getOtp(), OtpPurpose.REGISTER);
        if (!verified) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        Role role = request.getRole() == null ? Role.USER : request.getRole();
        if (role != Role.USER && role != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only USER and DIETICIAN accounts can be registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(defaultStatusFor(role))
                .emailVerified(true)
                .provider("LOCAL")
                .profileSetupCompleted(false)
                .subscriptionActive(false)
                .appointmentCompleted(false)
                .onboardingCompleted(false)
                .build();

        User savedUser = userRepository.save(user);
        auditLogService.record(
                savedUser.getId(),
                savedUser.getRole(),
                "USER_REGISTERED",
                "AUTH",
                "Registered " + savedUser.getRole() + " account " + savedUser.getId()
        );

        syncOnboardingState(savedUser);
        UserResponse userResponse = toUserResponse(savedUser);

        return AuthResponse.builder()
                .message("Registration successful")
                .token(jwtUtil.generateToken(savedUser))
                .userId(savedUser.getId())
                .role(savedUser.getRole())
                .onboardingCompleted(userResponse.isOnboardingCompleted())
                .subscriptionCompleted(userResponse.isSubscriptionCompleted())
                .hasActiveSubscription(userResponse.isHasActiveSubscription())
                .profileImageUrl(userResponse.getProfileImageUrl())
                .user(userResponse)
                .build();
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!otpService.isValidGmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid Gmail address.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role role = request.getRole() == null ? Role.USER : request.getRole();
        if (role != Role.USER && role != Role.DIETICIAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only USER and DIETICIAN accounts can be registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(defaultStatusFor(role))
                .emailVerified(false) // Needs verification
                .provider("LOCAL")
                .profileSetupCompleted(false)
                .subscriptionActive(false)
                .appointmentCompleted(false)
                .onboardingCompleted(false)
                .build();

        User savedUser = userRepository.save(user);
        auditLogService.record(
                savedUser.getId(),
                savedUser.getRole(),
                "USER_REGISTERED",
                "AUTH",
                "Registered " + savedUser.getRole() + " account " + savedUser.getId()
        );

        return AuthResponse.builder()
                .message("Registration successful")
                .user(toRegistrationResponse(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Login attempt received for email={}", email);

        if (!otpService.isValidGmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid Gmail address.");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.info("Login details - email: {}, found: false, role: null, status: null, passwordMatch: false", email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        migrateNullStatusToActive(user);
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("Login details - email: {}, found: true, role: {}, status: {}, passwordMatch: {}",
                email, user.getRole(), user.getStatus(), passwordMatches);

        if (!passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password mismatch");
        }

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please verify your Gmail before logging in.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account suspended");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account inactive");
        }

        syncOnboardingState(user);
        UserResponse userResponse = toUserResponse(user);
        auditLogService.record(
                user.getId(),
                user.getRole(),
                "LOGIN",
                "AUTH",
                "User " + user.getId() + " logged in"
        );

        return AuthResponse.builder()
                .message("Login successful")
                .token(jwtUtil.generateToken(user))
                .userId(user.getId())
                .role(user.getRole())
                .onboardingCompleted(userResponse.isOnboardingCompleted())
                .subscriptionCompleted(userResponse.isSubscriptionCompleted())
                .hasActiveSubscription(userResponse.isHasActiveSubscription())
                .profileImageUrl(userResponse.getProfileImageUrl())
                .user(userResponse)
                .build();
    }

    public UserResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        syncOnboardingState(user);
        return toUserResponse(user);
    }

    @Transactional
    public void sendForgotPasswordOtp(ForgotPasswordOtpRequest request) {
        String email = request.getIdentifier().trim().toLowerCase();
        if (!otpService.isValidGmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid Gmail address.");
        }
        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = otpService.generateAndSaveOtp(email, OtpPurpose.FORGOT_PASSWORD);
            String subject = "NutriCare Password Reset OTP";
            String body = "Your NutriCare password reset OTP is " + otp + ". It expires in 5 minutes.";
            emailService.sendEmail(email, subject, body);
        });
    }

    @Transactional
    public String verifyForgotPasswordOtp(VerifyPasswordOtpRequest request) {
        String email = request.getIdentifier().trim().toLowerCase();
        if (!otpService.isValidGmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid Gmail address.");
        }
        boolean verified = otpService.verifyOtp(email, request.getOtp(), OtpPurpose.FORGOT_PASSWORD);
        if (!verified) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(otpService.hash(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        passwordResetTokenRepository.save(resetToken);
        return rawToken;
    }

    @Transactional
    public void resetForgotPassword(ResetPasswordTokenRequest request) {
        String hashedToken = otpService.hash(request.getResetToken());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        auditLogService.record(user.getId(), user.getRole(), "PASSWORD_RESET", "AUTH", "Password reset via token");
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Google login is not configured yet.");
        }
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google credential token is required");
        }

        JsonNode tokenInfo = verifyGoogleToken(request.getIdToken());
        String audience = tokenInfo.path("aud").asText();
        if (!googleClientId.equals(audience)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token audience");
        }
        String email = tokenInfo.path("email").asText("").trim().toLowerCase();
        boolean emailVerified = tokenInfo.path("email_verified").asBoolean(false);
        if (email.isBlank() || !emailVerified) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
        }
        if (!otpService.isValidGmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only valid Gmail addresses are allowed.");
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName(tokenInfo.path("name").asText(email))
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .emailVerified(true)
                        .provider("GOOGLE")
                        .profileImage(tokenInfo.path("picture").asText(null))
                        .profileSetupCompleted(false)
                        .subscriptionActive(false)
                        .appointmentCompleted(false)
                        .onboardingCompleted(false)
                        .build()));

        if (!canAccessPlatform(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is not active. Please contact admin.");
        }

        syncOnboardingState(user);
        UserResponse userResponse = toUserResponse(user);
        return AuthResponse.builder()
                .message("Login successful")
                .token(jwtUtil.generateToken(user))
                .userId(user.getId())
                .role(user.getRole())
                .onboardingCompleted(userResponse.isOnboardingCompleted())
                .subscriptionCompleted(userResponse.isSubscriptionCompleted())
                .hasActiveSubscription(userResponse.isHasActiveSubscription())
                .profileImageUrl(userResponse.getProfileImageUrl())
                .user(userResponse)
                .build();
    }

    private UserResponse toUserResponse(User user) {
        SubscriptionStatus subscriptionStatus = subscriptionService.getSubscriptionStatus(user);
        boolean subscriptionActive = user.isSubscriptionActive() || subscriptionStatus == SubscriptionStatus.ACTIVE;
        boolean profileCompleted = user.isProfileSetupCompleted();
        boolean subscriptionCompleted = subscriptionActive;
        boolean appointmentCompleted = isAppointmentCompleted(user);
        boolean onboardingCompleted = isOnboardingCompleted(user, profileCompleted, subscriptionCompleted, appointmentCompleted);
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .subscriptionStatus(subscriptionStatus)
                .createdAt(user.getCreatedAt())
                .phone(user.getPhone())
                .age(user.getAge())
                .gender(user.getGender())
                .height(user.getHeight())
                .weight(user.getWeight())
                .goal(user.getGoal())
                .bio(user.getBio())
                .specialization(user.getSpecialization())
                .profileImage(user.getProfileImage())
                .profileImageUrl(user.getProfileImage())
                .profileSetupCompleted(user.isProfileSetupCompleted())
                .subscriptionActive(subscriptionActive)
                .profileCompleted(profileCompleted)
                .subscriptionCompleted(subscriptionCompleted)
                .hasActiveSubscription(subscriptionCompleted)
                .appointmentCompleted(appointmentCompleted)
                .onboardingCompleted(onboardingCompleted)
                .firstLogin(!onboardingCompleted)
                .bloodPressure(user.getBloodPressure())
                .sugarLevel(user.getSugarLevel())
                .activityLevel(user.getActivityLevel())
                .diseaseOrCondition(user.getDiseaseOrCondition())
                .allergies(user.getAllergies())
                .foodPreference(user.getFoodPreference())
                .degree(user.getDegree())
                .experience(user.getExperience())
                .location(user.getLocation())
                .consultationFee(user.getConsultationFee())
                .build();
    }

    private java.util.Optional<User> findByIdentifier(String identifier) {
        return looksLikeEmail(identifier)
                ? userRepository.findByEmail(identifier)
                : userRepository.findByPhone(identifier);
    }

    private PasswordResetOtp validOtp(String identifier, String otp) {
        return passwordResetOtpRepository
                .findFirstByIdentifierAndOtpHashAndUsedAtIsNullOrderByCreatedAtDesc(identifier, hash(otp))
                .filter(existing -> !existing.getExpiresAt().isBefore(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP"));
    }

    private String normalizeIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        return looksLikeEmail(value) ? value.toLowerCase() : value.replace(" ", "");
    }

    private boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }

    private String generateOtp() {
        return String.format("%06d", SecureRandomHolder.INSTANCE.nextInt(1_000_000));
    }

    private String hash(String value) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to hash OTP", ex);
        }
        return HexFormat.of().formatHex(digest);
    }

    private void sendOtpEmail(String to, String message) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            log.warn("Password reset email skipped because mail is not configured");
            return;
        }
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(mailFrom);
        email.setTo(to);
        email.setSubject("NutriCare password reset OTP");
        email.setText(message);
        mailSender.send(email);
    }

    private JsonNode verifyGoogleToken(String idToken) {
        try {
            String encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
            }
            return objectMapper.readTree(response.body());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify Google token", ex);
        }
    }

    private static final class SecureRandomHolder {
        private static final SecureRandom INSTANCE = new SecureRandom();
    }

    private UserResponse toRegistrationResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .profileSetupCompleted(user.isProfileSetupCompleted())
                .profileImage(user.getProfileImage())
                .profileImageUrl(user.getProfileImage())
                .subscriptionActive(user.isSubscriptionActive())
                .profileCompleted(user.isProfileSetupCompleted())
                .subscriptionCompleted(user.isSubscriptionActive())
                .hasActiveSubscription(user.isSubscriptionActive())
                .appointmentCompleted(user.isAppointmentCompleted())
                .onboardingCompleted(user.isOnboardingCompleted())
                .firstLogin(!user.isOnboardingCompleted())
                .build();
    }

    private UserStatus defaultStatusFor(Role role) {
        return UserStatus.ACTIVE;
    }

    private boolean canAccessPlatform(User user) {
        return user.getRole() != null
                && user.getStatus() == UserStatus.ACTIVE;
    }

    private void migrateNullStatusToActive(User user) {
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            log.info("Migrated null user status to ACTIVE for email={}", user.getEmail());
        }
    }

    private void syncOnboardingState(User user) {
        SubscriptionStatus subscriptionStatus = subscriptionService.getSubscriptionStatus(user);
        boolean subscriptionCompleted = user.isSubscriptionActive() || subscriptionStatus == SubscriptionStatus.ACTIVE;
        boolean appointmentCompleted = isAppointmentCompleted(user);
        boolean onboardingCompleted = isOnboardingCompleted(
                user,
                user.isProfileSetupCompleted(),
                subscriptionCompleted,
                appointmentCompleted
        );

        if (user.isAppointmentCompleted() != appointmentCompleted
                || user.isOnboardingCompleted() != onboardingCompleted
                || user.isSubscriptionActive() != subscriptionCompleted) {
            user.setAppointmentCompleted(appointmentCompleted);
            user.setOnboardingCompleted(onboardingCompleted);
            user.setSubscriptionActive(subscriptionCompleted);
            userRepository.save(user);
        }
    }

    private boolean isAppointmentCompleted(User user) {
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

    private boolean isOnboardingCompleted(
            User user,
            boolean profileCompleted,
            boolean subscriptionCompleted,
            boolean appointmentCompleted
    ) {
        if (user.getRole() == Role.USER || user.getRole() == Role.DIETICIAN) {
            return profileCompleted && subscriptionCompleted && appointmentCompleted;
        }
        return true;
    }
}
