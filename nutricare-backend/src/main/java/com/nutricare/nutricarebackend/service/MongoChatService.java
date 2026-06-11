package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.mongo.document.ChatMessageDocument;
import com.nutricare.nutricarebackend.mongo.repository.ChatMessageMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MongoChatService {

    private final ChatMessageMongoRepository chatMessageMongoRepository;

    public ChatMessageDocument saveMessage(
            Long conversationId,
            Long senderId,
            Long receiverId,
            Role senderRole,
            Role receiverRole,
            String message
    ) {
        return chatMessageMongoRepository.save(ChatMessageDocument.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .receiverId(receiverId)
                .senderRole(senderRole)
                .receiverRole(receiverRole)
                .message(message)
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public Optional<ChatMessageDocument> findById(String id) {
        return id == null || id.isBlank() ? Optional.empty() : chatMessageMongoRepository.findById(id);
    }

    public Optional<ChatMessageDocument> findLastMessage(Long conversationId) {
        return chatMessageMongoRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    public List<ChatMessageDocument> findMessages(Long conversationId) {
        return chatMessageMongoRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public ChatMessageDocument save(ChatMessageDocument document) {
        return chatMessageMongoRepository.save(document);
    }
}
