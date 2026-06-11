package com.nutricare.nutricarebackend.config;

import com.nutricare.nutricarebackend.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAccessInterceptor implements HandlerInterceptor {

    private final SubscriptionService subscriptionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String feature = featureForPath(request.getRequestURI());
        log.debug("Checking subscription feature access: path={}, feature={}, user={}", request.getRequestURI(), feature, authentication.getName());
        subscriptionService.requireFeature(authentication.getName(), feature);

        return true;
    }

    private String featureForPath(String path) {
        if (path == null) {
            return "BOOK_APPOINTMENT";
        }
        if (path.contains("/chat/")) {
            return "CHAT";
        }
        if (path.contains("/diet-plans/") || path.contains("/weekly-meal-plans/")) {
            return "MEAL_LOGS";
        }
        if (path.contains("/meal-compliance/")) {
            return "FOLLOW_UPS";
        }
        if (path.contains("/appointments/") && path.endsWith("/meeting")) {
            return "VIDEO_CALL";
        }
        if (path.contains("/appointments/")) {
            return "BOOK_APPOINTMENT";
        }
        return "BOOK_APPOINTMENT";
    }
}
