package com.nutricare.nutricarebackend.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.sms.from:}")
    private String smsFrom;

    @PostConstruct
    public void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.warn("TwilioService: accountSid or authToken is missing. Twilio will not be initialized.");
            return;
        }
        try {
            Twilio.init(accountSid.trim(), authToken.trim());
            log.info("TwilioService initialized successfully with account SID: {}", accountSid.trim());
        } catch (Exception e) {
            log.error("Failed to initialize Twilio: {}", e.getMessage(), e);
        }
    }

    public String sendSms(String toPhoneNumber, String message) {
        boolean sidExists = accountSid != null && !accountSid.isBlank();
        boolean tokenExists = authToken != null && !authToken.isBlank();
        log.info("Twilio Send SMS Attempt: Account SID exists = {}, Auth Token exists = {}, SMS From number = '{}'",
                sidExists, tokenExists, smsFrom);

        log.info("Twilio SMS recipient phone before normalization: '{}'", toPhoneNumber);

        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.error("Twilio SMS rejected: recipient phone number is null or empty.");
            throw new IllegalArgumentException("Recipient phone number is null or empty");
        }

        if (!sidExists || !tokenExists) {
            log.error("Twilio SMS failed: Missing Twilio credentials (SID/Token).");
            throw new IllegalStateException("Missing Twilio credentials");
        }

        String normalizedTo = normalizeSmsPhone(toPhoneNumber);
        log.info("Twilio SMS recipient phone after normalization: '{}'", normalizedTo);

        if (normalizedTo == null || normalizedTo.isBlank() || !normalizedTo.matches("^\\+\\d{10,15}$")) {
            log.error("Twilio SMS rejected: invalid phone number format '{}' (normalized: '{}')", toPhoneNumber, normalizedTo);
            throw new IllegalArgumentException("Invalid phone number format: " + toPhoneNumber);
        }

        String normalizedFrom = normalizeSmsPhone(smsFrom);
        if (normalizedFrom == null || normalizedFrom.isBlank()) {
            log.error("Twilio SMS failed: Twilio SMS From number is not configured.");
            throw new IllegalStateException("Twilio SMS From number is not configured");
        }

        try {
            // Force initialize if needed
            Twilio.init(accountSid.trim(), authToken.trim());

            Message msg = Message.creator(
                    new PhoneNumber(normalizedTo),
                    new PhoneNumber(normalizedFrom),
                    message
            ).create();

            String sid = msg.getSid();
            if (sid == null || sid.isBlank()) {
                log.error("Twilio SMS failed: message created but no SID returned by Twilio.");
                throw new RuntimeException("Twilio returned an empty message SID");
            }

            log.info("Twilio SMS sent successfully. Message SID: {}", sid);
            return sid;
        } catch (Exception e) {
            log.error("Twilio SMS failed to send to '{}'. Twilio error: {}", normalizedTo, e.getMessage());
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Twilio exception occurred", e);
        }
    }

    public String normalizeSmsPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        // Remove spaces
        String cleaned = phone.trim().replaceAll("\\s+", "");
        
        // Remove other common formatting chars like dashes and parentheses
        cleaned = cleaned.replaceAll("[\\-()]+", "");
        
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        
        // If 10 digits and numbers only, prefix +91
        if (cleaned.length() == 10 && cleaned.matches("\\d+")) {
            return "+91" + cleaned;
        }
        
        // If 12 digits starting with 91, prefix +
        if (cleaned.length() == 12 && cleaned.startsWith("91") && cleaned.matches("\\d+")) {
            return "+" + cleaned;
        }
        
        return "+" + cleaned;
    }
}
