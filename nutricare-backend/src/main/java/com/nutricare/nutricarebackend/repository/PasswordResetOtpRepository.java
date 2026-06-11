package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.PasswordResetOtp;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    List<PasswordResetOtp> findByUserAndUsedAtIsNull(User user);

    Optional<PasswordResetOtp> findFirstByIdentifierAndOtpHashAndUsedAtIsNullOrderByCreatedAtDesc(String identifier, String otpHash);

    Optional<PasswordResetOtp> findFirstByIdentifierAndVerifiedAtIsNotNullAndUsedAtIsNullOrderByCreatedAtDesc(String identifier);
}
