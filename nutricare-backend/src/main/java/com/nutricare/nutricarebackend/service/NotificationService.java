package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.NotificationResponse;
import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.Notification;
import com.nutricare.nutricarebackend.entity.NotificationLog;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.mongo.document.NotificationLogDocument;
import com.nutricare.nutricarebackend.repository.DietPlanRepository;
import com.nutricare.nutricarebackend.repository.NotificationLogRepository;
import com.nutricare.nutricarebackend.repository.NotificationRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_WHATSAPP = "WHATSAPP";
    public static final String CHANNEL_IN_APP = "IN_APP";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final MongoNotificationLogService mongoNotificationLogService;
    private final UserRepository userRepository;
    private final DietPlanRepository dietPlanRepository;
    private final AuditLogService auditLogService;
    private final TwilioService twilioService;

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String twilioWhatsappFrom;

    public NotificationResponse create(User receiver, User sender, String title, String message, String type) {
        Notification notification = Notification.builder()
                .receiverId(receiver.getId())
                .receiverRole(receiver.getRole())
                .senderId(sender.getId())
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationLogDocument doc = mongoNotificationLogService.createLog(
                receiver.getId(),
                receiver.getRole(),
                normalizeIndianPhone(receiver.getPhone()),
                CHANNEL_IN_APP,
                truncate(title, 255),
                truncate(message, 2000),
                STATUS_SENT
        );
        if (doc != null) {
            doc.setReceiverName(receiver.getFullName());
            doc.setReceiverEmail(receiver.getEmail());
            mongoNotificationLogService.save(doc);
        }
        return toResponse(saved);
    }

    public void sendInAppNotification(User receiver, User sender, String title, String message, String type) {
        if (receiver == null || !isReceivable(receiver)) {
            return;
        }
        if (sender != null) {
            create(receiver, sender, title, message, type);
        }
    }

    public void sendNotification(User receiver, User sender, String title, String message, String type) {
        if (receiver == null || !isReceivable(receiver)) {
            return;
        }
        if (sender != null) {
            create(receiver, sender, title, message, type);
        }
        sendToUserPreferredChannels(receiver, title, message);
    }

    public void sendToUserPreferredChannels(User receiver, String title, String message) {
        if (receiver == null || !isReceivable(receiver)) {
            return;
        }
        sendSms(receiver, title, message);
    }

    public NotificationLog sendSms(User receiver, String title, String message) {
        NotificationLog logEntry = createPendingLog(receiver, CHANNEL_SMS, title, message);
        if (receiver == null) {
            return markSkipped(logEntry, "Receiver is missing");
        }
        if (!receiver.isSmsNotificationsEnabled()) {
            return markSkipped(logEntry, "SMS notifications are disabled");
        }
        if (!hasText(receiver.getPhone())) {
            return markSkipped(logEntry, "Receiver phone number is missing");
        }
        try {
            String fullMessage = makeFullMessage(title, message);
            String sid = twilioService.sendSms(receiver.getPhone(), fullMessage);
            return markSent(logEntry, sid);
        } catch (Exception ex) {
            log.error("SMS notification failed for user {}: {}", receiver.getId(), ex.getMessage());
            RuntimeException runtimeException = (ex instanceof RuntimeException) ? (RuntimeException) ex : new RuntimeException(ex.getMessage(), ex);
            return markFailed(logEntry, runtimeException);
        }
    }

    public NotificationLog sendWhatsApp(User receiver, String title, String message) {
        NotificationLog logEntry = createPendingLog(receiver, CHANNEL_WHATSAPP, title, message);
        if (receiver == null) {
            return markSkipped(logEntry, "Receiver is missing");
        }
        if (!receiver.isWhatsappNotificationsEnabled()) {
            return markSkipped(logEntry, "WhatsApp notifications are disabled");
        }
        if (!hasText(receiver.getPhone())) {
            return markSkipped(logEntry, "Receiver phone number is missing");
        }
        try {
            Message sentMessage = sendTwilioMessage(
                    whatsappAddress(receiver.getPhone()),
                    twilioWhatsappFrom,
                    makeFullMessage(title, message)
            );
            return markSent(logEntry, sentMessage.getSid());
        } catch (RuntimeException ex) {
            return markFailed(logEntry, ex);
        }
    }

    public void sendOnce(User receiver, User sender, String title, String message, String type) {
        if (receiver == null || alreadyLogged(receiver, title, message)) {
            return;
        }
        sendNotification(receiver, sender, title, message, type);
    }

    public boolean alreadyLogged(User user, String title, String message) {
        return user != null && (mongoNotificationLogService.exists(user.getId(), title, message)
                || notificationLogRepository.existsByUserIdAndTitleAndMessage(user.getId(), title, message));
    }

    public List<NotificationResponse> getMyNotifications(String email) {
        User user = getUserByEmail(email);
        ensureWeeklyReviewNotifications(user);

        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount(String email) {
        User user = getUserByEmail(email);
        return notificationRepository.countByReceiverIdAndReadFalse(user.getId());
    }

    public NotificationResponse markRead(String email, Long id) {
        User user = getUserByEmail(email);
        Notification notification = notificationRepository.findByIdAndReceiverId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    public List<NotificationResponse> markAllRead(String email) {
        User user = getUserByEmail(email);
        List<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(notification -> notification.setRead(true));

        return notificationRepository.saveAll(notifications)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationLog createPendingLog(User receiver, String channel, String title, String message) {
        if (receiver == null) {
            throw new IllegalArgumentException("Receiver is required");
        }
        LocalDateTime now = LocalDateTime.now();
        NotificationLogDocument document = mongoNotificationLogService.createLog(
                receiver.getId(),
                receiver.getRole(),
                normalizeIndianPhone(receiver.getPhone()),
                channel,
                truncate(title, 255),
                truncate(message, 2000),
                STATUS_PENDING
        );
        document.setReceiverName(receiver.getFullName());
        document.setReceiverEmail(receiver.getEmail());

        NotificationLog log = notificationLogRepository.save(NotificationLog.builder()
                .userId(receiver.getId())
                .receiverRole(receiver.getRole())
                .channel(channel)
                .title(truncate(title, 255))
                .message(truncate(message, 2000))
                .status(STATUS_PENDING)
                .mongoDocumentId(document.getId())
                .build());
        document.setMysqlLogId(log.getId());
        document.setUpdatedAt(now);
        mongoNotificationLogService.save(document);
        return log;
    }

    private NotificationLog markSent(NotificationLog logEntry, String twilioSid) {
        logEntry.setStatus(STATUS_SENT);
        logEntry.setTwilioSid(twilioSid);
        NotificationLog saved = notificationLogRepository.save(logEntry);
        findLogDocument(saved).ifPresent(document -> {
            document.setStatus(STATUS_SENT);
            document.setTwilioSid(twilioSid);
            document.setUpdatedAt(LocalDateTime.now());
            mongoNotificationLogService.save(document);
        });
        auditLogService.record(
                null,
                null,
                "TWILIO_NOTIFICATION_SENT",
                "NOTIFICATIONS",
                logEntry.getChannel() + " notification sent to user " + logEntry.getUserId()
        );
        log.info("Notification sent userId={}, channel={}, sid={}", logEntry.getUserId(), logEntry.getChannel(), twilioSid);
        return saved;
    }

    private NotificationLog markFailed(NotificationLog logEntry, RuntimeException ex) {
        logEntry.setStatus(STATUS_FAILED);
        NotificationLog saved = notificationLogRepository.save(logEntry);
        findLogDocument(saved).ifPresent(document -> {
            document.setStatus(STATUS_FAILED);
            document.setErrorType(ex.getClass().getSimpleName());
            document.setErrorMessage(truncate(ex.getMessage(), 1000));
            document.setUpdatedAt(LocalDateTime.now());
            mongoNotificationLogService.save(document);
        });
        auditLogService.record(
                null,
                null,
                "TWILIO_NOTIFICATION_FAILED",
                "NOTIFICATIONS",
                logEntry.getChannel() + " notification failed for user " + logEntry.getUserId()
        );
        log.warn("Notification failed userId={}, channel={}, error={}",
                logEntry.getUserId(),
                logEntry.getChannel(),
                ex.getMessage()
        );
        return saved;
    }

    private NotificationLog markSkipped(NotificationLog logEntry, String reason) {
        logEntry.setStatus(STATUS_SKIPPED);
        NotificationLog saved = notificationLogRepository.save(logEntry);
        findLogDocument(saved).ifPresent(document -> {
            document.setStatus(STATUS_SKIPPED);
            document.setErrorMessage(truncate(reason, 1000));
            document.setUpdatedAt(LocalDateTime.now());
            mongoNotificationLogService.save(document);
        });
        auditLogService.record(
                null,
                null,
                "TWILIO_NOTIFICATION_SKIPPED",
                "NOTIFICATIONS",
                logEntry.getChannel() + " notification skipped for user " + logEntry.getUserId()
        );
        log.info("Notification skipped userId={}, channel={}, reason={}", logEntry.getUserId(), logEntry.getChannel(), reason);
        return saved;
    }

    private Message sendTwilioMessage(String to, String from, String message) {
        requireTwilioConfigured(from);
        if (!hasText(to)) {
            throw new IllegalArgumentException("Receiver phone number is missing");
        }
        return Message.creator(
                new PhoneNumber(normalizePhone(to)),
                new PhoneNumber(normalizePhone(from)),
                message
        ).create();
    }

    private void requireTwilioConfigured(String from) {
        if (!hasText(accountSid) || !hasText(authToken) || !hasText(from)) {
            throw new IllegalStateException("Twilio configuration is incomplete");
        }
    }

    private void requireReceiverPhone(User receiver) {
        if (receiver == null || !hasText(receiver.getPhone())) {
            throw new IllegalArgumentException("Receiver phone number is missing");
        }
    }

    private boolean isReceivable(User user) {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    private String whatsappAddress(String phone) {
        if (!hasText(phone)) {
            return "";
        }
        String stripped = phone.trim();
        if (stripped.startsWith("whatsapp:")) {
            stripped = stripped.substring("whatsapp:".length());
        }
        return "whatsapp:" + normalizeIndianPhone(stripped);
    }

    private String normalizeIndianPhone(String phone) {
        if (!hasText(phone)) {
            return null;
        }
        String cleaned = phone.trim().replaceAll("[\\s\\-()]+", "");
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        if (cleaned.length() == 12 && cleaned.startsWith("91")) {
            return "+" + cleaned;
        }
        if (cleaned.length() == 10) {
            return "+91" + cleaned;
        }
        log.warn("Unable to normalize phone number: {}", phone);
        return cleaned;
    }

    private String normalizePhone(String phone) {
        if (!hasText(phone)) {
            return "";
        }
        return phone.trim().replaceAll("[\\s\\-()]+", "");
    }

    private String makeFullMessage(String title, String message) {
        if (message.startsWith("NutriCare:")) {
            return message;
        }
        return "NutriCare: " + title + "\n" + message;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void ensureWeeklyReviewNotifications(User user) {
        if (user.getRole() == Role.USER) {
            dietPlanRepository.findByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .filter(this::isReviewDue)
                    .forEach(plan -> {
                        createIfMissing(
                                user,
                                plan.getDietician(),
                                "Weekly Nutrition Review Due",
                                "Your weekly nutrition review is due.\nSchedule your follow-up appointment with your dietician.",
                                "WEEKLY_REVIEW_DUE"
                        );
                        createIfMissing(
                                plan.getDietician(),
                                user,
                                "Weekly Review Due",
                                "Weekly review due for " + user.getFullName() + ".\nPlease schedule follow-up consultation.",
                                "WEEKLY_REVIEW_DUE"
                        );
                    });
        } else if (user.getRole() == Role.DIETICIAN) {
            dietPlanRepository.findAll()
                    .stream()
                    .filter(plan -> plan.getDietician().getId().equals(user.getId()))
                    .filter(this::isReviewDue)
                    .forEach(plan -> createIfMissing(
                            user,
                            plan.getUser(),
                            "Weekly Review Due",
                            "Weekly review due for " + plan.getUser().getFullName() + ".\nPlease schedule follow-up consultation.",
                            "WEEKLY_REVIEW_DUE"
                    ));
        }
    }

    private boolean isReviewDue(DietPlan plan) {
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        long dayNumber = ChronoUnit.DAYS.between(plan.getStartDate(), today) + 1;
        return !today.isBefore(plan.getStartDate())
                && !today.isAfter(plan.getEndDate())
                && (dayNumber == 6 || dayNumber == 7);
    }

    private void createIfMissing(User receiver, User sender, String title, String message, String type) {
        if (notificationRepository.existsByReceiverIdAndTypeAndMessage(receiver.getId(), type, message)) {
            return;
        }
        create(receiver, sender, title, message, type);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .receiverId(notification.getReceiverId())
                .receiverRole(notification.getReceiverRole())
                .recipientId(notification.getReceiverId())
                .recipientRole(notification.getReceiverRole())
                .senderId(notification.getSenderId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private Optional<NotificationLogDocument> findLogDocument(NotificationLog logEntry) {
        if (logEntry.getMongoDocumentId() == null || logEntry.getMongoDocumentId().isBlank()) {
            return Optional.empty();
        }
        return mongoNotificationLogService.findById(logEntry.getMongoDocumentId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
