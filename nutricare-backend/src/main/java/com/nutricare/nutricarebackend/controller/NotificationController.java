package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.NotificationResponse;
import com.nutricare.nutricarebackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getMyNotifications(authentication.getName()));
    }

    @GetMapping("/api/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getMyNotifications(authentication.getName()));
    }

    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(authentication.getName())));
    }

    @PutMapping("/api/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(authentication.getName(), id));
    }

    @PutMapping("/api/notifications/read-all")
    public ResponseEntity<List<NotificationResponse>> markAllRead(Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAllRead(authentication.getName()));
    }
}
