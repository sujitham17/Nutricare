package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.mongo.document.NotificationLogDocument;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioNotificationService {

    private static final String TEST_TITLE = "NutriCare Test";

    private final NotificationService notificationService;
    private final MongoNotificationLogService mongoNotificationLogService;
    private final TwilioService twilioService;

    public void sendSms(User receiver, String title, String message) {
        notificationService.sendSms(receiver, title, message);
    }

    public void sendToUserPreferredChannels(User receiver, String title, String message) {
        notificationService.sendToUserPreferredChannels(receiver, title, message);
    }

    public TwilioTestResult sendTestSms(@NotBlank String to, @NotBlank String message) {
        String channel = NotificationService.CHANNEL_SMS;
        try {
            String sid = twilioService.sendSms(to, message);
            persistTestLog(null, Role.ADMIN, to, channel, TEST_TITLE, message, "SENT", null, sid);
            return new TwilioTestResult(true, sid, null);
        } catch (Exception ex) {
            String error = ex.getMessage() == null ? "Twilio send failed" : ex.getMessage();
            persistTestLog(null, Role.ADMIN, to, channel, TEST_TITLE, message, "FAILED", error, null);
            log.warn("Twilio test {} failed to {}: {}", channel, maskPhoneNumber(to), error);
            return new TwilioTestResult(false, null, error);
        }
    }

    private void persistTestLog(Long receiverId, Role receiverRole, String receiverPhone, String channel, String title, String message, String status, String errorMessage, String twilioSid) {
        NotificationLogDocument document = NotificationLogDocument.builder()
                .receiverId(receiverId)
                .receiverRole(receiverRole)
                .receiverPhone(receiverPhone)
                .receiverName("Admin Test")
                .channel(channel)
                .title(title)
                .message(message)
                .status(status)
                .errorMessage(errorMessage)
                .twilioSid(twilioSid)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        mongoNotificationLogService.save(document);
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    public record TwilioTestResult(boolean success, String sid, String errorMessage) {
    }
}
