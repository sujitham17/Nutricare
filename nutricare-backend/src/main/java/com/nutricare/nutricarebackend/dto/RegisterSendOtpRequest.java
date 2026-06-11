package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterSendOtpRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    @Size(min = 6)
    private String password;

    private Role role;
}
