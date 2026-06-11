package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatContactResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String profileImageUrl;
    private UserStatus status;
    private Long conversationId;
    private String lastMessage;
}
