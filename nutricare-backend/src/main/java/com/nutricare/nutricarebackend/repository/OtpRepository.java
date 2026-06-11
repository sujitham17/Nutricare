package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.Otp;
import com.nutricare.nutricarebackend.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    List<Otp> findByEmailAndPurposeAndVerifiedIsFalse(String email, OtpPurpose purpose);

    Optional<Otp> findFirstByEmailAndOtpHashAndPurposeAndVerifiedIsFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, String otpHash, OtpPurpose purpose, LocalDateTime now);

    Optional<Otp> findFirstByEmailAndOtpHashAndPurposeAndVerifiedIsTrueAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, String otpHash, OtpPurpose purpose, LocalDateTime now);
}
