package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.ChatConversation;
import com.nutricare.nutricarebackend.entity.ConversationType;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    List<ChatConversation> findByUserOrDieticianOrAdminOrderByCreatedAtDesc(User user, User dietician, User admin);

    Optional<ChatConversation> findByConversationTypeAndUserAndDietician(
            ConversationType conversationType,
            User user,
            User dietician
    );

    Optional<ChatConversation> findByConversationTypeAndUserAndAdmin(
            ConversationType conversationType,
            User user,
            User admin
    );

    Optional<ChatConversation> findByConversationTypeAndDieticianAndAdmin(
            ConversationType conversationType,
            User dietician,
            User admin
    );
}
