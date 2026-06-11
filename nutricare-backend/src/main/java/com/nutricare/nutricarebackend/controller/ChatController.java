package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.ChatContactResponse;
import com.nutricare.nutricarebackend.dto.ChatConversationResponse;
import com.nutricare.nutricarebackend.dto.ChatMessageResponse;
import com.nutricare.nutricarebackend.dto.ChatSendRequest;
import com.nutricare.nutricarebackend.service.ChatService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/api/chat/contacts")
    public ResponseEntity<List<ChatContactResponse>> getContacts(Authentication authentication) {
        subscriptionService.requireFeature(authentication.getName(), "CHAT");
        return ResponseEntity.ok(chatService.getContacts(authentication.getName()));
    }

    @GetMapping("/api/chat/conversations")
    public ResponseEntity<List<ChatConversationResponse>> getConversations(Authentication authentication) {
        subscriptionService.requireFeature(authentication.getName(), "CHAT");
        return ResponseEntity.ok(chatService.getConversations(authentication.getName()));
    }

    @GetMapping("/api/chat/conversations/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long conversationId
    ) {
        subscriptionService.requireFeature(authentication.getName(), "CHAT");
        return ResponseEntity.ok(chatService.getMessages(authentication.getName(), conversationId));
    }

    @PostMapping("/api/chat/send")
    public ResponseEntity<ChatMessageResponse> send(
            Authentication authentication,
            @Valid @RequestBody ChatSendRequest request
    ) {
        subscriptionService.requireFeature(authentication.getName(), "CHAT");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(chatService.send(authentication.getName(), request));
    }
}
