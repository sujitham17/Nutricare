package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BillController {

    private final BillService billService;

    @GetMapping("/api/payments/{paymentId}/bill")
    public ResponseEntity<byte[]> getPaymentBill(
            Authentication authentication,
            @PathVariable Long paymentId
    ) {
        log.info("Requesting consultation payment bill: paymentId={}, user={}", paymentId, authentication.getName());
        byte[] pdfBytes = billService.getBillPdfByPayment(paymentId, authentication.getName());
        return buildPdfResponse(pdfBytes, "nutricare-payment-bill-" + paymentId + ".pdf");
    }

    @GetMapping("/api/subscriptions/{subscriptionId}/bill")
    public ResponseEntity<byte[]> getSubscriptionBill(
            Authentication authentication,
            @PathVariable Long subscriptionId
    ) {
        log.info("Requesting subscription bill: subscriptionId={}, user={}", subscriptionId, authentication.getName());
        byte[] pdfBytes = billService.getBillPdfBySubscription(subscriptionId, authentication.getName());
        return buildPdfResponse(pdfBytes, "nutricare-subscription-bill-" + subscriptionId + ".pdf");
    }

    @GetMapping("/api/appointments/{appointmentId}/bill")
    public ResponseEntity<byte[]> getAppointmentBill(
            Authentication authentication,
            @PathVariable Long appointmentId
    ) {
        log.info("Requesting appointment consultation bill: appointmentId={}, user={}", appointmentId, authentication.getName());
        byte[] pdfBytes = billService.getBillPdfByAppointment(appointmentId, authentication.getName());
        return buildPdfResponse(pdfBytes, "nutricare-appointment-bill-" + appointmentId + ".pdf");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
