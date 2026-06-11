package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Otp;
import com.nutricare.nutricarebackend.entity.OtpPurpose;
import com.nutricare.nutricarebackend.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    public boolean isValidGmail(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim().toLowerCase();
        return trimmed.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$");
    }

    @Transactional
    public String generateAndSaveOtp(String email, OtpPurpose purpose) {
        String cleanEmail = email.trim().toLowerCase();
        
        // Deactivate previous OTPs
        List<Otp> pending = otpRepository.findByEmailAndPurposeAndVerifiedIsFalse(cleanEmail, purpose);
        if (!pending.isEmpty()) {
            pending.forEach(o -> o.setVerified(true));
            otpRepository.saveAll(pending);
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hashedOtp = hash(otp);

        Otp otpEntity = Otp.builder()
                .email(cleanEmail)
                .otpHash(hashedOtp)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(otpEntity);
        log.info("Generated OTP for email={}, purpose={}", cleanEmail, purpose);
        return otp;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp, OtpPurpose purpose) {
        if (email == null || otp == null) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();
        String hashedOtp = hash(otp.trim());

        Optional<Otp> otpOpt = otpRepository
                .findFirstByEmailAndOtpHashAndPurposeAndVerifiedIsFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        cleanEmail, hashedOtp, purpose, LocalDateTime.now());

        if (otpOpt.isPresent()) {
            Otp otpEntity = otpOpt.get();
            otpEntity.setVerified(true);
            otpRepository.save(otpEntity);
            log.info("Successfully verified OTP for email={}, purpose={}", cleanEmail, purpose);
            return true;
        }
        return false;
    }

    public String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to hash OTP", ex);
        }
    }
}
