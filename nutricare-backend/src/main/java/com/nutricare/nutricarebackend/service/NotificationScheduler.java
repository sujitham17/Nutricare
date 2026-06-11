package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.SubscriptionStatus;
import com.nutricare.nutricarebackend.entity.SubscriptionTransaction;
import com.nutricare.nutricarebackend.repository.DietPlanRepository;
import com.nutricare.nutricarebackend.repository.SubscriptionTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final DietPlanRepository dietPlanRepository;
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendWeeklyReviewReminders() {
        LocalDate today = LocalDate.now();
        dietPlanRepository.findAll()
                .stream()
                .filter(plan -> isWeeklyReviewDue(plan, today))
                .forEach(plan -> {
                    String message = "Weekly nutrition review is due.\n\nPlease schedule a follow-up consultation.";
                    notificationService.sendOnce(
                            plan.getUser(),
                            plan.getDietician(),
                            "Weekly Review Reminder",
                            message,
                            "WEEKLY_REVIEW_REMINDER"
                    );
                    notificationService.sendOnce(
                            plan.getDietician(),
                            plan.getUser(),
                            "Weekly Review Reminder",
                            message,
                            "WEEKLY_REVIEW_REMINDER"
                    );
                });
        log.info("Weekly review reminders processed");
    }

    @Scheduled(cron = "0 30 9 * * *")
    public void sendSubscriptionExpiryReminders() {
        LocalDate expiryDate = LocalDate.now().plusDays(7);
        subscriptionTransactionRepository.findAll()
                .stream()
                .filter(this::isActiveSubscription)
                .filter(subscription -> expiryDate.equals(subscription.getEndDate()))
                .forEach(subscription -> notificationService.sendOnce(
                        subscription.getUser(),
                        subscription.getUser(),
                        "Subscription Expiry Reminder",
                        "Your NutriCare subscription expires in 7 days.\n\nPlease renew to continue services.",
                        "SUBSCRIPTION_EXPIRY_REMINDER"
                ));
        log.info("Subscription expiry reminders processed for {}", expiryDate);
    }

    private boolean isWeeklyReviewDue(DietPlan plan, LocalDate today) {
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            return false;
        }
        long dayNumber = ChronoUnit.DAYS.between(plan.getStartDate(), today) + 1;
        return !today.isBefore(plan.getStartDate())
                && !today.isAfter(plan.getEndDate())
                && (dayNumber == 6 || dayNumber == 7);
    }

    private boolean isActiveSubscription(SubscriptionTransaction subscription) {
        return subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                && subscription.getEndDate() != null;
    }
}
