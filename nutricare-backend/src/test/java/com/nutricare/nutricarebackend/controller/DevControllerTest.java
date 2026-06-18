package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.UserStatus;
import com.nutricare.nutricarebackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

class DevControllerTest {

    private DevController controller;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new DevController(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(controller, "devEndpointsEnabled", true);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));
    }

    @Test
    void resetsPasswordForExistingAdmin() {
        User existingUser = new User();
        existingUser.setId(10L);
        existingUser.setEmail("nutricareadmin@gmail.com");
        existingUser.setFullName("Existing Admin");
        existingUser.setRole(Role.USER);
        existingUser.setStatus(UserStatus.SUSPENDED);
        existingUser.setEmailVerified(false);

        when(userRepository.findByEmail("nutricareadmin@gmail.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = controller.resetAdminPassword();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getId()).isEqualTo(10L);
        assertThat(savedUser.getEmail()).isEqualTo("nutricareadmin@gmail.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded_87654321");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.isEmailVerified()).isTrue();
    }

    @Test
    void createsAndResetsPasswordForNewAdmin() {
        when(userRepository.findByEmail("nutricareadmin@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = controller.resetAdminPassword();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getId()).isNull();
        assertThat(savedUser.getEmail()).isEqualTo("nutricareadmin@gmail.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded_87654321");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.isEmailVerified()).isTrue();
    }

    @Test
    void returnsNotFoundWhenDisabled() {
        ReflectionTestUtils.setField(controller, "devEndpointsEnabled", false);
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.resetAdminPassword()
        );
    }
}
