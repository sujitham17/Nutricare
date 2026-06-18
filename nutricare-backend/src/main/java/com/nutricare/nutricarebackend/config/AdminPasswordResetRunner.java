package com.nutricare.nutricarebackend.config;

import com.nutricare.nutricarebackend.entity.ChatConversation;
import com.nutricare.nutricarebackend.entity.ChatMessage;
import com.nutricare.nutricarebackend.entity.ConversationType;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.mongo.document.ChatMessageDocument;
import com.nutricare.nutricarebackend.repository.ChatConversationRepository;
import com.nutricare.nutricarebackend.repository.ChatMessageRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.nutricare.nutricarebackend.service.MongoChatService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminPasswordResetRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MongoChatService mongoChatService;
    private final EntityManager entityManager;

    @Value("${nutricare.dev.endpoints.enabled:false}")
    private boolean devEndpointsEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!devEndpointsEnabled) {
            log.info("AdminPasswordResetRunner is disabled.");
            return;
        }
        log.info("Running AdminPasswordResetRunner startup password reset...");
        try {
            User user = userRepository.findByEmail("nutricareadmin@gmail.com")
                    .orElseGet(() -> {
                        log.info("Admin user not found. Creating a new one.");
                        return User.builder()
                                .email("nutricareadmin@gmail.com")
                                .fullName("NutriCare Admin")
                                .provider("LOCAL")
                                .profileSetupCompleted(false)
                                .subscriptionActive(false)
                                .appointmentCompleted(false)
                                .onboardingCompleted(false)
                                .build();
                    });

            user.setPassword(passwordEncoder.encode("87654321"));
            user.setRole(Role.ADMIN);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);

            User currentAdmin = userRepository.save(user);
            log.info("AdminPasswordResetRunner successfully updated/reset admin user nutricareadmin@gmail.com.");

            // Run migration for old admin if they exist
            migrateOldAdmin(currentAdmin);
        } catch (Exception e) {
            log.error("Failed to reset admin password on startup: {}", e.getMessage(), e);
        }
    }

    private void migrateOldAdmin(User currentAdmin) {
        log.info("Checking for old admin user 'admin@nutricare.com' to migrate...");
        Optional<User> oldAdminOpt = userRepository.findByEmail("admin@nutricare.com");
        if (oldAdminOpt.isEmpty()) {
            log.info("Old admin user 'admin@nutricare.com' not found. No migration needed.");
            return;
        }

        User oldAdmin = oldAdminOpt.get();
        log.info("Migrating data from old admin (ID: {}) to current admin (ID: {})...", oldAdmin.getId(), currentAdmin.getId());

        // 1. Find all chat conversations involving the old admin
        List<ChatConversation> conversations = chatConversationRepository.findByUserOrDieticianOrAdminOrderByCreatedAtDesc(null, null, oldAdmin);
        log.info("Found {} conversations involving the old admin to migrate.", conversations.size());

        for (ChatConversation oldConv : conversations) {
            Optional<ChatConversation> newConvOpt = Optional.empty();
            if (oldConv.getConversationType() == ConversationType.USER_ADMIN) {
                User user = oldConv.getUser();
                if (user != null) {
                    newConvOpt = chatConversationRepository.findByConversationTypeAndUserAndAdmin(
                            ConversationType.USER_ADMIN, user, currentAdmin);
                }
            } else if (oldConv.getConversationType() == ConversationType.DIETICIAN_ADMIN) {
                User dietician = oldConv.getDietician();
                if (dietician != null) {
                    newConvOpt = chatConversationRepository.findByConversationTypeAndDieticianAndAdmin(
                            ConversationType.DIETICIAN_ADMIN, dietician, currentAdmin);
                }
            }

            if (newConvOpt.isPresent()) {
                ChatConversation newConv = newConvOpt.get();
                log.info("Conversation already exists for current admin (ID: {}). Merging messages from old conversation (ID: {})...", newConv.getId(), oldConv.getId());

                // Migrate MySQL messages
                List<ChatMessage> mysqlMessages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(oldConv);
                for (ChatMessage msg : mysqlMessages) {
                    msg.setConversation(newConv);
                    if (msg.getSender().getId().equals(oldAdmin.getId())) {
                        msg.setSender(currentAdmin);
                    }
                    if (msg.getReceiver().getId().equals(oldAdmin.getId())) {
                        msg.setReceiver(currentAdmin);
                    }
                }
                chatMessageRepository.saveAll(mysqlMessages);

                // Migrate Mongo documents
                List<ChatMessageDocument> mongoDocs = mongoChatService.findMessages(oldConv.getId());
                for (ChatMessageDocument doc : mongoDocs) {
                    doc.setConversationId(newConv.getId());
                    if (oldAdmin.getId().equals(doc.getSenderId())) {
                        doc.setSenderId(currentAdmin.getId());
                    }
                    if (oldAdmin.getId().equals(doc.getReceiverId())) {
                        doc.setReceiverId(currentAdmin.getId());
                    }
                    mongoChatService.save(doc);
                }

                // Delete the old conversation
                chatConversationRepository.delete(oldConv);
            } else {
                log.info("No existing conversation for current admin. Pointing old conversation (ID: {}) to current admin...", oldConv.getId());
                oldConv.setAdmin(currentAdmin);
                chatConversationRepository.save(oldConv);

                // Update messages' sender/receiver if they were old admin
                List<ChatMessage> mysqlMessages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(oldConv);
                for (ChatMessage msg : mysqlMessages) {
                    if (msg.getSender().getId().equals(oldAdmin.getId())) {
                        msg.setSender(currentAdmin);
                    }
                    if (msg.getReceiver().getId().equals(oldAdmin.getId())) {
                        msg.setReceiver(currentAdmin);
                    }
                }
                chatMessageRepository.saveAll(mysqlMessages);

                // Update Mongo documents
                List<ChatMessageDocument> mongoDocs = mongoChatService.findMessages(oldConv.getId());
                for (ChatMessageDocument doc : mongoDocs) {
                    if (oldAdmin.getId().equals(doc.getSenderId())) {
                        doc.setSenderId(currentAdmin.getId());
                    }
                    if (oldAdmin.getId().equals(doc.getReceiverId())) {
                        doc.setReceiverId(currentAdmin.getId());
                    }
                    mongoChatService.save(doc);
                }
            }
        }

        // 2. Migrate other log/notification entities
        int updatedLogs = entityManager.createQuery("UPDATE AdminActionLog a SET a.adminId = :newId WHERE a.adminId = :oldId")
                .setParameter("oldId", oldAdmin.getId())
                .setParameter("newId", currentAdmin.getId())
                .executeUpdate();
        log.info("Updated {} AdminActionLog records.", updatedLogs);

        int updatedNotifSenders = entityManager.createQuery("UPDATE Notification n SET n.senderId = :newId WHERE n.senderId = :oldId")
                .setParameter("oldId", oldAdmin.getId())
                .setParameter("newId", currentAdmin.getId())
                .executeUpdate();
        log.info("Updated {} Notification sender records.", updatedNotifSenders);

        int updatedNotifReceivers = entityManager.createQuery("UPDATE Notification n SET n.receiverId = :newId WHERE n.receiverId = :oldId")
                .setParameter("oldId", oldAdmin.getId())
                .setParameter("newId", currentAdmin.getId())
                .executeUpdate();
        log.info("Updated {} Notification receiver records.", updatedNotifReceivers);

        int updatedNotifLogs = entityManager.createQuery("UPDATE NotificationLog n SET n.userId = :newId WHERE n.userId = :oldId")
                .setParameter("oldId", oldAdmin.getId())
                .setParameter("newId", currentAdmin.getId())
                .executeUpdate();
        log.info("Updated {} NotificationLog records.", updatedNotifLogs);

        // 3. Delete old admin user
        userRepository.delete(oldAdmin);
        log.info("Successfully deleted old admin user 'admin@nutricare.com' and completed migration.");
    }
}
