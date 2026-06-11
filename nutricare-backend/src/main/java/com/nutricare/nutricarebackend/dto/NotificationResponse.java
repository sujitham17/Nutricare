package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private Long receiverId;
    private Role receiverRole;
    private Long recipientId;
    private Role recipientRole;
    private Long senderId;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
}
