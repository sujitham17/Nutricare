package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogResponse {

    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverEmail;
    private Role receiverRole;
    private String channel;
    private String title;
    private String message;
    private String status;
    private String twilioSid;
    private String receiverPhone;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
