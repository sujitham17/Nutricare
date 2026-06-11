package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.ChatContactResponse;
import com.nutricare.nutricarebackend.dto.ChatConversationResponse;
import com.nutricare.nutricarebackend.dto.ChatMessageResponse;
import com.nutricare.nutricarebackend.dto.ChatSendRequest;
import com.nutricare.nutricarebackend.entity.ChatConversation;
import com.nutricare.nutricarebackend.entity.ChatMessage;
import com.nutricare.nutricarebackend.entity.ConversationType;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.mongo.document.ChatMessageDocument;
import com.nutricare.nutricarebackend.repository.ChatConversationRepository;
import com.nutricare.nutricarebackend.repository.ChatMessageRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppointmentRepository appointmentRepository;
    private final MongoChatService mongoChatService;
    private final AuditLogService auditLogService;
    private final SubscriptionService subscriptionService;

    public List<ChatContactResponse> getContacts(String email) {
        User current = getUserByEmail(email);
        requireChatAvailable(current);
        Map<Long, User> contacts = new LinkedHashMap<>();

        if (current.getRole() == Role.USER) {
            appointmentRepository.findByUserOrderByAppointmentDateDescAppointmentTimeDesc(current)
                    .stream()
                    .map(appointment -> appointment.getDietician())
                    .filter(contact -> contact != null && isChatAvailable(contact))
                    .forEach(contact -> contacts.putIfAbsent(contact.getId(), contact));
            userRepository.findByRoleOrderByCreatedAtDesc(Role.ADMIN)
                    .stream()
                    .filter(this::isChatAvailable)
                    .forEach(contact -> contacts.putIfAbsent(contact.getId(), contact));
        } else if (current.getRole() == Role.DIETICIAN) {
            appointmentRepository.findByDieticianOrderByAppointmentDateDescAppointmentTimeDesc(current)
                    .stream()
                    .map(appointment -> appointment.getUser())
                    .filter(contact -> contact != null && isChatAvailable(contact))
                    .forEach(contact -> contacts.putIfAbsent(contact.getId(), contact));
            userRepository.findByRoleOrderByCreatedAtDesc(Role.ADMIN)
                    .stream()
                    .filter(this::isChatAvailable)
                    .forEach(contact -> contacts.putIfAbsent(contact.getId(), contact));
        } else if (current.getRole() == Role.ADMIN) {
            userRepository.findByRoleInOrderByCreatedAtDesc(List.of(Role.USER, Role.DIETICIAN))
                    .stream()
                    .filter(this::isChatAvailable)
                    .forEach(contact -> contacts.putIfAbsent(contact.getId(), contact));
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role cannot use chat");
        }

        return contacts.values()
                .stream()
                .map(contact -> toContactResponse(current, contact))
                .toList();
    }

    public List<ChatConversationResponse> getConversations(String email) {
        User current = getUserByEmail(email);
        subscriptionService.requireFeature(current, "CHAT");

        return chatConversationRepository.findByUserOrDieticianOrAdminOrderByCreatedAtDesc(current, current, current)
                .stream()
                .filter(conversation -> canAccessConversation(current, conversation))
                .map(conversation -> toConversationResponse(current, conversation))
                .toList();
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(String email, Long conversationId) {
        User current = getUserByEmail(email);
        subscriptionService.requireFeature(current, "CHAT");
        ChatConversation conversation = getConversation(conversationId);
        requireParticipant(current, conversation);
        requireConversationAccess(current, conversation);

        List<ChatMessageDocument> mongoMessages = mongoChatService.findMessages(conversation.getId());
        if (!mongoMessages.isEmpty()) {
            LocalDateTime readAt = LocalDateTime.now();
            mongoMessages.stream()
                    .filter(message -> current.getId().equals(message.getReceiverId()) && !message.isReadStatus())
                    .forEach(message -> {
                        message.setReadStatus(true);
                        message.setReadAt(readAt);
                        mongoChatService.save(message);
                    });

            List<ChatMessage> mysqlMessages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation);
            mysqlMessages.stream()
                    .filter(message -> message.getReceiver().getId().equals(current.getId()) && !message.isReadStatus())
                    .forEach(message -> message.setReadStatus(true));
            chatMessageRepository.saveAll(mysqlMessages);

            return mongoMessages.stream()
                    .map(document -> toMessageResponse(document, conversation))
                    .toList();
        }

        List<ChatMessage> messages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        LocalDateTime readAt = LocalDateTime.now();
        messages.stream()
                .filter(message -> message.getReceiver().getId().equals(current.getId()) && !message.isReadStatus())
                .forEach(message -> {
                    message.setReadStatus(true);
                    findMessageDocument(message).ifPresent(document -> {
                        document.setReadStatus(true);
                        document.setReadAt(readAt);
                        mongoChatService.save(document);
                    });
                });
        chatMessageRepository.saveAll(messages);

        return messages.stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse send(String email, ChatSendRequest request) {
        User sender = getUserByEmail(email);
        subscriptionService.requireFeature(sender, "CHAT");
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found"));
        requireAllowedContact(sender, receiver);

        ChatConversation conversation = getOrCreateConversation(sender, receiver);
        ChatMessageDocument document = mongoChatService.saveMessage(
                conversation.getId(),
                sender.getId(),
                receiver.getId(),
                sender.getRole(),
                receiver.getRole(),
                request.getMessage()
        );

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .message(truncate(request.getMessage(), 2000))
                .mongoDocumentId(document.getId())
                .readStatus(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        document.setMysqlMessageId(saved.getId());
        mongoChatService.save(document);
        auditLogService.record(
                sender.getId(),
                sender.getRole(),
                "CHAT_MESSAGE_SENT",
                "CHAT",
                "Message sent in conversation " + conversation.getId() + " to user " + receiver.getId()
        );

        return toMessageResponse(saved);
    }

    private ChatConversation getOrCreateConversation(User sender, User receiver) {
        if (sender.getRole() == Role.USER && receiver.getRole() == Role.DIETICIAN) {
            return chatConversationRepository.findByConversationTypeAndUserAndDietician(ConversationType.USER_DIETICIAN, sender, receiver)
                    .orElseGet(() -> createConversation(ConversationType.USER_DIETICIAN, sender, receiver, null));
        }

        if (sender.getRole() == Role.DIETICIAN && receiver.getRole() == Role.USER) {
            return chatConversationRepository.findByConversationTypeAndUserAndDietician(ConversationType.USER_DIETICIAN, receiver, sender)
                    .orElseGet(() -> createConversation(ConversationType.USER_DIETICIAN, receiver, sender, null));
        }

        if (sender.getRole() == Role.USER && receiver.getRole() == Role.ADMIN) {
            return chatConversationRepository.findByConversationTypeAndUserAndAdmin(ConversationType.USER_ADMIN, sender, receiver)
                    .orElseGet(() -> createConversation(ConversationType.USER_ADMIN, sender, null, receiver));
        }

        if (sender.getRole() == Role.ADMIN && receiver.getRole() == Role.USER) {
            return chatConversationRepository.findByConversationTypeAndUserAndAdmin(ConversationType.USER_ADMIN, receiver, sender)
                    .orElseGet(() -> createConversation(ConversationType.USER_ADMIN, receiver, null, sender));
        }

        if (sender.getRole() == Role.DIETICIAN && receiver.getRole() == Role.ADMIN) {
            return chatConversationRepository.findByConversationTypeAndDieticianAndAdmin(ConversationType.DIETICIAN_ADMIN, sender, receiver)
                    .orElseGet(() -> createConversation(ConversationType.DIETICIAN_ADMIN, null, sender, receiver));
        }

        if (sender.getRole() == Role.ADMIN && receiver.getRole() == Role.DIETICIAN) {
            return chatConversationRepository.findByConversationTypeAndDieticianAndAdmin(ConversationType.DIETICIAN_ADMIN, receiver, sender)
                    .orElseGet(() -> createConversation(ConversationType.DIETICIAN_ADMIN, null, receiver, sender));
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This chat contact is not allowed");
    }

    private ChatConversation createConversation(ConversationType type, User user, User dietician, User admin) {
        return chatConversationRepository.save(ChatConversation.builder()
                .conversationType(type)
                .user(user)
                .dietician(dietician)
                .admin(admin)
                .build());
    }

    private ChatConversation getConversation(Long conversationId) {
        return chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private void requireParticipant(User current, ChatConversation conversation) {
        boolean participant = isSame(current, conversation.getUser())
                || isSame(current, conversation.getDietician())
                || isSame(current, conversation.getAdmin());

        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another conversation");
        }
    }

    private boolean isSame(User a, User b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ChatContactResponse toContactResponse(User current, User contact) {
        ChatConversation conversation = findConversation(current, contact);
        String lastMessage = conversation == null ? null : chatMessageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation)
                .map(this::messageText)
                .or(() -> mongoChatService.findLastMessage(conversation.getId())
                        .map(ChatMessageDocument::getMessage))
                .orElse(null);

        return ChatContactResponse.builder()
                .id(contact.getId())
                .fullName(contact.getRole() == Role.ADMIN ? "NutriCare Admin" : contact.getFullName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .role(contact.getRole())
                .profileImageUrl(contact.getProfileImage())
                .status(contact.getStatus())
                .conversationId(conversation == null ? null : conversation.getId())
                .lastMessage(lastMessage)
                .build();
    }

    private boolean canAccessConversation(User current, ChatConversation conversation) {
        if (!isChatAvailable(current)) {
            return false;
        }
        User contact = getConversationContact(current, conversation);
        return isChatAvailable(contact) && isAllowedContact(current, contact);
    }

    private void requireConversationAccess(User current, ChatConversation conversation) {
        if (!canAccessConversation(current, conversation)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This chat contact is not available");
        }
    }

    private void requireAllowedContact(User current, User contact) {
        requireChatAvailable(current);
        requireChatAvailable(contact);
        if (current.getId().equals(contact.getId()) || !isAllowedContact(current, contact)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This chat contact is not allowed");
        }
    }

    private void requireChatAvailable(User user) {
        if (user.getRole() != Role.ADMIN) {
            subscriptionService.requireFeature(user, "CHAT");
        }
        if (!isChatAvailable(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This chat account is not active");
        }
    }

    private boolean isAllowedContact(User current, User contact) {
        if (current.getRole() == Role.ADMIN) {
            return contact.getRole() == Role.USER || contact.getRole() == Role.DIETICIAN;
        }
        if (current.getRole() == Role.USER) {
            return contact.getRole() == Role.DIETICIAN || contact.getRole() == Role.ADMIN;
        }
        if (current.getRole() == Role.DIETICIAN) {
            return contact.getRole() == Role.USER || contact.getRole() == Role.ADMIN;
        }
        return false;
    }

    private boolean isChatAvailable(User user) {
        if (user == null) {
            return false;
        }
        UserStatus status = user.getStatus();
        return status == null || status == UserStatus.ACTIVE || status == UserStatus.APPROVED;
    }

    private ChatConversation findConversation(User current, User contact) {
        if (current.getRole() == Role.USER && contact.getRole() == Role.DIETICIAN) {
            return chatConversationRepository.findByConversationTypeAndUserAndDietician(ConversationType.USER_DIETICIAN, current, contact)
                    .orElse(null);
        }
        if (current.getRole() == Role.DIETICIAN && contact.getRole() == Role.USER) {
            return chatConversationRepository.findByConversationTypeAndUserAndDietician(ConversationType.USER_DIETICIAN, contact, current)
                    .orElse(null);
        }
        if (current.getRole() == Role.USER && contact.getRole() == Role.ADMIN) {
            return chatConversationRepository.findByConversationTypeAndUserAndAdmin(ConversationType.USER_ADMIN, current, contact)
                    .orElse(null);
        }
        if (current.getRole() == Role.ADMIN && contact.getRole() == Role.USER) {
            return chatConversationRepository.findByConversationTypeAndUserAndAdmin(ConversationType.USER_ADMIN, contact, current)
                    .orElse(null);
        }
        if (current.getRole() == Role.DIETICIAN && contact.getRole() == Role.ADMIN) {
            return chatConversationRepository.findByConversationTypeAndDieticianAndAdmin(ConversationType.DIETICIAN_ADMIN, current, contact)
                    .orElse(null);
        }
        if (current.getRole() == Role.ADMIN && contact.getRole() == Role.DIETICIAN) {
            return chatConversationRepository.findByConversationTypeAndDieticianAndAdmin(ConversationType.DIETICIAN_ADMIN, contact, current)
                    .orElse(null);
        }
        return null;
    }

    private ChatConversationResponse toConversationResponse(User current, ChatConversation conversation) {
        User contact = getConversationContact(current, conversation);
        String lastMessage = chatMessageRepository.findFirstByConversationOrderByCreatedAtDesc(conversation)
                .map(this::messageText)
                .or(() -> mongoChatService.findLastMessage(conversation.getId())
                        .map(ChatMessageDocument::getMessage))
                .orElse(null);

        return ChatConversationResponse.builder()
                .id(conversation.getId())
                .conversationType(conversation.getConversationType())
                .contact(toContactResponse(current, contact))
                .lastMessage(lastMessage)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private User getConversationContact(User current, ChatConversation conversation) {
        if (!isSame(current, conversation.getUser()) && conversation.getUser() != null) {
            return conversation.getUser();
        }
        if (!isSame(current, conversation.getDietician()) && conversation.getDietician() != null) {
            return conversation.getDietician();
        }
        if (!isSame(current, conversation.getAdmin()) && conversation.getAdmin() != null) {
            return conversation.getAdmin();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation has no contact for current user");
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageDocument document = findMessageDocument(message).orElse(null);
        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .receiverId(message.getReceiver().getId())
                .receiverName(message.getReceiver().getFullName())
                .message(document == null ? message.getMessage() : document.getMessage())
                .readStatus(document == null ? message.isReadStatus() : document.isReadStatus())
                .createdAt(document == null || document.getCreatedAt() == null ? message.getCreatedAt() : document.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessageDocument document, ChatConversation conversation) {
        User sender = userRepository.findById(document.getSenderId()).orElse(null);
        User receiver = userRepository.findById(document.getReceiverId()).orElse(null);
        return ChatMessageResponse.builder()
                .id(document.getMysqlMessageId())
                .conversationId(conversation.getId())
                .senderId(document.getSenderId())
                .senderName(sender == null ? null : sender.getFullName())
                .receiverId(document.getReceiverId())
                .receiverName(receiver == null ? null : receiver.getFullName())
                .message(document.getMessage())
                .readStatus(document.isReadStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }

    private Optional<ChatMessageDocument> findMessageDocument(ChatMessage message) {
        if (message.getMongoDocumentId() == null || message.getMongoDocumentId().isBlank()) {
            return Optional.empty();
        }
        return mongoChatService.findById(message.getMongoDocumentId());
    }

    private String messageText(ChatMessage message) {
        return findMessageDocument(message)
                .map(ChatMessageDocument::getMessage)
                .orElse(message.getMessage());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
