package com.nutricare.nutricarebackend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentPaymentVerifyRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bindsExactAppointmentVerificationPayload() throws Exception {
        String json = """
                {
                  "paymentId": 11,
                  "appointmentId": 22,
                  "razorpayOrderId": "order_123",
                  "razorpayPaymentId": "pay_123",
                  "razorpaySignature": "signature_123"
                }
                """;

        AppointmentPaymentVerifyRequest request = objectMapper.readValue(json, AppointmentPaymentVerifyRequest.class);

        assertThat(request.getPaymentId()).isEqualTo(11L);
        assertThat(request.getAppointmentId()).isEqualTo(22L);
        assertThat(request.getRazorpayOrderId()).isEqualTo("order_123");
        assertThat(request.getRazorpayPaymentId()).isEqualTo("pay_123");
        assertThat(request.getRazorpaySignature()).isEqualTo("signature_123");
    }

    @Test
    void legacySnakeCaseRazorpayFieldsDoNotSatisfyAppointmentDto() {
        String json = """
                {
                  "appointmentId": 22,
                  "razorpay_order_id": "order_123",
                  "razorpay_payment_id": "pay_123",
                  "razorpay_signature": "signature_123"
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, AppointmentPaymentVerifyRequest.class))
                .hasMessageContaining("Unrecognized field \"razorpay_order_id\"");
    }
}
