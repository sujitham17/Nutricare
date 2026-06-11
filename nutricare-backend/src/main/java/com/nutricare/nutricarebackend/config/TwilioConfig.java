package com.nutricare.nutricarebackend.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TwilioConfig {

    @Value("${twilio.account.sid:${twilio.account-sid:}}")
    private String accountSid;

    @Value("${twilio.auth.token:${twilio.auth-token:}}")
    private String authToken;

    @Value("${twilio.sms.from:${twilio.sms-from:}}")
    private String smsFrom;

    @PostConstruct
    public void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.warn("NutriCare Twilio is not configured. SMS/WhatsApp notifications will be disabled.");
            return;
        }
        Twilio.init(accountSid.trim(), authToken.trim());
        
        // Validate SMS sender number
        if (smsFrom == null || smsFrom.isBlank()) {
            log.error("Invalid Twilio sender number configured: SMS sender is missing");
            return;
        }
        
        String normalized = smsFrom.trim().replace(" ", "");
        if (!normalized.startsWith("+")) {
            log.error("Invalid Twilio sender number configured: {} does not start with +", smsFrom);
            return;
        }
        
        log.info("NutriCare Twilio initialized with SMS sender: {}", maskPhoneNumber(normalized));
    }
    
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
