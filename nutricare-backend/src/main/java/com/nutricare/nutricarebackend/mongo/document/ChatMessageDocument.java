package com.nutricare.nutricarebackend.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.nutricare.nutricarebackend.entity.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@CompoundIndex(name = "idx_chat_conversation_created", def = "{'conversationId': 1, 'createdAt': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDocument {

    @Id
    private String id;

    @Indexed
    private Long mysqlMessageId;

    @Indexed
    private Long conversationId;

    private Long senderId;
    private Long receiverId;
    private Role senderRole;
    private Role receiverRole;
    private String message;
    private boolean readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
