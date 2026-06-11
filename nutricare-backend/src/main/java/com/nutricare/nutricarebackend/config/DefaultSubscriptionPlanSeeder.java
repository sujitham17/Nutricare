package com.nutricare.nutricarebackend.config;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionPlan;
import com.nutricare.nutricarebackend.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DefaultSubscriptionPlanSeeder implements ApplicationRunner {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    private static final Map<Role, Set<String>> REQUIRED_PLANS = Map.of(
            Role.USER, Set.of("Basic", "Premium", "Pro"),
            Role.DIETICIAN, Set.of("Starter", "Professional", "Enterprise")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(Role.USER, "Basic", "Appointment booking and one-hour video calls", "Appointments,Video calls up to 60 minutes", "499.00",
                true, true, 60, false, false, false, null, null, null);
        seed(Role.USER, "Premium", "Meal logs, follow-ups, booking and one-hour video calls", "Appointments,Video calls up to 60 minutes,Meal logs,Follow-ups", "999.00",
                true, true, 60, true, true, false, null, null, null);
        seed(Role.USER, "Pro", "Complete NutriCare access", "Appointments,Unlimited video calls,Meal logs,Follow-ups,Chat", "1499.00",
                true, true, null, true, true, true, null, null, null);

        seed(Role.DIETICIAN, "Starter", "Up to 5 user appointments", "Bookings,Video calls,Basic user handling", "499.00",
                true, true, 60, false, false, false, "Basic", 5, 5);
        seed(Role.DIETICIAN, "Professional", "6 to 10 user appointments", "Bookings,Meal logs,Follow-ups,Premium and Pro users", "999.00",
                true, true, 60, true, true, false, "Premium,Pro", 10, 10);
        seed(Role.DIETICIAN, "Enterprise", "Unlimited / more than 10 user appointments", "Bookings,Meal logs,Follow-ups,Chat,Unlimited users", "1999.00",
                true, true, null, true, true, true, "Basic,Premium,Pro", null, -1);
        deactivateNonRequiredPlans(Role.USER);
        deactivateNonRequiredPlans(Role.DIETICIAN);
    }

    private void seed(
            Role audience,
            String name,
            String description,
            String features,
            String price,
            boolean canBookAppointment,
            boolean canVideoCall,
            Integer videoCallLimitMinutes,
            boolean canMealLogs,
            boolean canFollowUps,
            boolean canChat,
            String allowedUserPlans,
            Integer maxUsers,
            Integer maxAppointments
    ) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanNameAndPlanAudience(name, audience)
                .orElseGet(SubscriptionPlan::new);

        plan.setPlanName(name);
        plan.setPlanAudience(audience);
        plan.setDescription(description);
        plan.setPrice(new BigDecimal(price));
        plan.setDurationInDays(30);
        plan.setFeatures(features);
        plan.setActive(true);
        plan.setCanBookAppointment(canBookAppointment);
        plan.setCanVideoCall(canVideoCall);
        plan.setVideoCallLimitMinutes(videoCallLimitMinutes);
        plan.setCanMealLogs(canMealLogs);
        plan.setCanFollowUps(canFollowUps);
        plan.setCanChat(canChat);
        plan.setAllowedUserPlans(allowedUserPlans);
        plan.setMaxUsers(maxUsers);
        plan.setMaxAppointments(maxAppointments);

        subscriptionPlanRepository.save(plan);
    }

    private void deactivateNonRequiredPlans(Role audience) {
        Set<String> requiredNames = REQUIRED_PLANS.get(audience);
        subscriptionPlanRepository.findByPlanAudienceAndActiveTrueOrderByPriceAsc(audience)
                .stream()
                .filter(plan -> !requiredNames.contains(plan.getPlanName()))
                .forEach(plan -> {
                    plan.setActive(false);
                    subscriptionPlanRepository.save(plan);
                });
    }
}
