package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.ChatConversation;
import com.nutricare.nutricarebackend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationOrderByCreatedAtAsc(ChatConversation conversation);

    Optional<ChatMessage> findFirstByConversationOrderByCreatedAtDesc(ChatConversation conversation);
}
