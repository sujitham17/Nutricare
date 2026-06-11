package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.ConversationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatConversationResponse {

    private Long id;
    private ConversationType conversationType;
    private ChatContactResponse contact;
    private String lastMessage;
    private LocalDateTime createdAt;
}
