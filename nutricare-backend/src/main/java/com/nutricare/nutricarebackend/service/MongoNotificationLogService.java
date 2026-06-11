package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.mongo.document.NotificationLogDocument;
import com.nutricare.nutricarebackend.mongo.repository.NotificationLogMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MongoNotificationLogService {

    private final NotificationLogMongoRepository notificationLogMongoRepository;

    public NotificationLogDocument createLog(Long receiverId, Role receiverRole, String receiverPhone, String channel, String title, String message, String status) {
        return notificationLogMongoRepository.save(NotificationLogDocument.builder()
                .receiverId(receiverId)
                .receiverRole(receiverRole)
                .receiverPhone(receiverPhone)
                .channel(channel)
                .title(title)
                .message(message)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    public NotificationLogDocument save(NotificationLogDocument document) {
        document.setUpdatedAt(LocalDateTime.now());
        return notificationLogMongoRepository.save(document);
    }

    public Optional<NotificationLogDocument> findById(String id) {
        return id == null || id.isBlank() ? Optional.empty() : notificationLogMongoRepository.findById(id);
    }

    public boolean exists(Long receiverId, String title, String message) {
        return notificationLogMongoRepository.existsByReceiverIdAndTitleAndMessage(receiverId, title, message);
    }

    public List<NotificationLogDocument> findAll() {
        return notificationLogMongoRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<NotificationLogDocument> findByFilters(Long receiverId, String channel, String status) {
        boolean hasReceiverId = receiverId != null;
        boolean hasChannel = hasText(channel);
        boolean hasStatus = hasText(status);

        if (hasReceiverId && hasChannel && hasStatus) {
            return notificationLogMongoRepository.findByReceiverIdAndChannelAndStatusOrderByCreatedAtDesc(receiverId, channel, status);
        }
        if (hasReceiverId && hasChannel) {
            return notificationLogMongoRepository.findByReceiverIdAndChannelOrderByCreatedAtDesc(receiverId, channel);
        }
        if (hasReceiverId && hasStatus) {
            return notificationLogMongoRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(receiverId, status);
        }
        if (hasChannel && hasStatus) {
            return notificationLogMongoRepository.findByChannelAndStatusOrderByCreatedAtDesc(channel, status);
        }
        if (hasReceiverId) {
            return notificationLogMongoRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId);
        }
        if (hasChannel) {
            return notificationLogMongoRepository.findByChannelOrderByCreatedAtDesc(channel);
        }
        if (hasStatus) {
            return notificationLogMongoRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return findAll();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
