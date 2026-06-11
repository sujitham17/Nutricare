package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.AppointmentPaymentVerifyRequest;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentVerifyRequest;
import com.nutricare.nutricarebackend.service.ConsultationPaymentService;
import com.nutricare.nutricarebackend.service.SubscriptionService;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.nutricare.nutricarebackend.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultationPaymentControllerVerifyAppointmentTest {

    private ConsultationPaymentService consultationPaymentService;
    private ConsultationPaymentController controller;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consultationPaymentService = mock(ConsultationPaymentService.class);
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        controller = new ConsultationPaymentController(
                consultationPaymentService,
                subscriptionService,
                userRepository,
                subscriptionPlanRepository
        );
    }

    @Test
    void mapsAppointmentVerifyPayloadToConsultationVerifyRequest() {
        AppointmentPaymentVerifyRequest request = appointmentVerifyRequest();
        Authentication authentication = authentication();
        when(consultationPaymentService.verifyPayment(eq("patient@example.com"), any(ConsultationPaymentVerifyRequest.class)))
                .thenReturn(ConsultationPaymentResponse.builder()
                        .success(true)
                        .message("Appointment confirmed. Dietician has been notified.")
                        .build());

        ResponseEntity<?> response = controller.verifyAppointmentPayment(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<ConsultationPaymentVerifyRequest> captor = ArgumentCaptor.forClass(ConsultationPaymentVerifyRequest.class);
        verify(consultationPaymentService).verifyPayment(eq("patient@example.com"), captor.capture());
        ConsultationPaymentVerifyRequest mappedRequest = captor.getValue();
        assertThat(mappedRequest.getPaymentType()).isEqualTo("APPOINTMENT");
        assertThat(mappedRequest.getPaymentId()).isEqualTo(11L);
        assertThat(mappedRequest.getAppointmentId()).isEqualTo(22L);
        assertThat(mappedRequest.getRazorpayOrderId()).isEqualTo("order_123");
        assertThat(mappedRequest.getRazorpayPaymentId()).isEqualTo("pay_123");
        assertThat(mappedRequest.getRazorpaySignature()).isEqualTo("signature_123");
    }

    @Test
    void returnsBadRequestWhenServiceRejectsInvalidSignature() {
        AppointmentPaymentVerifyRequest request = appointmentVerifyRequest();
        Authentication authentication = authentication();
        when(consultationPaymentService.verifyPayment(eq("patient@example.com"), any(ConsultationPaymentVerifyRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature"));

        ResponseEntity<?> response = controller.verifyAppointmentPayment(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("success")).isEqualTo(false);
        assertThat(body.get("message")).isEqualTo("Invalid Razorpay signature");
    }

    @Test
    void returnsExactMissingPaymentIdBeforeServiceCall() {
        AppointmentPaymentVerifyRequest request = appointmentVerifyRequest();
        request.setPaymentId(null);

        ResponseEntity<?> response = controller.verifyAppointmentPayment(authentication(), request);

        assertError(response, HttpStatus.BAD_REQUEST, "Missing paymentId");
        verify(consultationPaymentService, never()).verifyPayment(any(), any());
    }

    @Test
    void returnsExactMissingRazorpaySignatureBeforeServiceCall() {
        AppointmentPaymentVerifyRequest request = appointmentVerifyRequest();
        request.setRazorpaySignature(" ");

        ResponseEntity<?> response = controller.verifyAppointmentPayment(authentication(), request);

        assertError(response, HttpStatus.BAD_REQUEST, "Missing razorpaySignature");
        verify(consultationPaymentService, never()).verifyPayment(any(), any());
    }

    private void assertError(ResponseEntity<?> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("success")).isEqualTo(false);
        assertThat(body.get("message")).isEqualTo(message);
    }

    private AppointmentPaymentVerifyRequest appointmentVerifyRequest() {
        AppointmentPaymentVerifyRequest request = new AppointmentPaymentVerifyRequest();
        request.setPaymentId(11L);
        request.setAppointmentId(22L);
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("signature_123");
        return request;
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("patient@example.com");
        return authentication;
    }
}
