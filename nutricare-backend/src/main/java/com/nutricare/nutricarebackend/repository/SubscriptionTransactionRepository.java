package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.SubscriptionTransaction;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionTransactionRepository extends JpaRepository<SubscriptionTransaction, Long> {

    Optional<SubscriptionTransaction> findByIdAndUser(Long id, User user);

    Optional<SubscriptionTransaction> findByUserAndPlanIdAndRazorpayOrderIdAndStatus(
            User user,
            Long planId,
            String razorpayOrderId,
            PaymentStatus status
    );

    Optional<SubscriptionTransaction> findFirstByUserAndSubscriptionStatusOrderByCreatedAtDesc(
            User user,
            SubscriptionStatus subscriptionStatus
    );

    List<SubscriptionTransaction> findByUserAndSubscriptionStatus(User user, SubscriptionStatus subscriptionStatus);

    List<SubscriptionTransaction> findAllByOrderByCreatedAtDesc();

    List<SubscriptionTransaction> findByDeletedFalseOrderByCreatedAtDesc();

    long countBySubscriptionStatus(SubscriptionStatus subscriptionStatus);

    long countByRoleTypeAndSubscriptionStatus(Role roleType, SubscriptionStatus subscriptionStatus);

    List<SubscriptionTransaction> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
}
