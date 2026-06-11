package com.nutricare.nutricarebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private Boolean success;
    private String reason;
    private String errorCode;
    private LocalDateTime timestamp;
    private String path;

    public static ApiErrorResponse of(String reason) {
        return ApiErrorResponse.builder()
                .success(false)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiErrorResponse of(String reason, String errorCode) {
        return ApiErrorResponse.builder()
                .success(false)
                .reason(reason)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
