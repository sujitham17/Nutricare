package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSendRequest {

    @NotNull
    private Long receiverId;

    @NotBlank
    private String message;
}
